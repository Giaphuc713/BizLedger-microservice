using Confluent.Kafka;
using Microsoft.AspNetCore.SignalR;
using MongoDB.Driver;
using notification_service.Data;
using notification_service.Models;
using System.Text.Json;

namespace notification_service.Messaging
{
    public class KafkaConsumerService : BackgroundService
    {
        private readonly IServiceProvider _serviceProvider;
        private readonly IHubContext<NotificationHub> _hubContext;
        private readonly ILogger<KafkaConsumerService> _logger;
        private readonly string[] _topics = { "order.events", "inventory.events", "payment.events" };
        private readonly string _bootstrapServers;
        private readonly string _groupId = "notification-group";

        public KafkaConsumerService(
            IServiceProvider serviceProvider,
            IHubContext<NotificationHub> hubContext,
            ILogger<KafkaConsumerService> logger,
            Microsoft.Extensions.Configuration.IConfiguration configuration)
        {
            _serviceProvider = serviceProvider;
            _hubContext = hubContext;
            _logger = logger;
            _bootstrapServers = configuration["Kafka:BootstrapServers"] ?? "localhost:9092";
        }

        protected override async Task ExecuteAsync(CancellationToken cancellationToken)
        {
            await Task.Yield(); // yield thread to let startup check complete

            var config = new ConsumerConfig
            {
                BootstrapServers = _bootstrapServers,
                GroupId = _groupId,
                AutoOffsetReset = AutoOffsetReset.Earliest,
                EnableAutoCommit = false // Manual commit for at-least-once with inbox deduplication
            };

            using var consumer = new ConsumerBuilder<string, string>(config).Build();
            foreach (var topic in _topics)
            {
                consumer.Subscribe(topic);
            }
            _logger.LogInformation("[Kafka Consumer] Subscribed to topics: {Topics}", string.Join(", ", _topics));

            while (!cancellationToken.IsCancellationRequested)
            {
                try
                {
                    var result = consumer.Consume(TimeSpan.FromMilliseconds(500));
                    if (result == null) continue;

                    _logger.LogInformation("[Kafka Consumer] Received raw message on topic {Topic}: {Message}",
                        result.Topic, result.Message.Value);

                    var envelope = JsonSerializer.Deserialize<EventEnvelope>(result.Message.Value);
                    if (envelope != null)
                    {
                        await ProcessEnvelopeWithDeduplicationAsync(envelope, result.Topic);
                    }

                    // Commit offset
                    consumer.Commit(result);
                }
                catch (ConsumeException ex)
                {
                    _logger.LogError(ex, "Kafka consumption exception");
                    await Task.Delay(2000, cancellationToken);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Unexpected error processing message");
                    await Task.Delay(2000, cancellationToken);
                }
            }

            consumer.Close();
        }

        private async Task ProcessEnvelopeWithDeduplicationAsync(EventEnvelope envelope, string originTopic)
        {
            using var scope = _serviceProvider.CreateScope();
            var dbContext = scope.ServiceProvider.GetRequiredService<MongoDbContext>();

            // IDEMPOTENCY check (Inbox Pattern)
            bool exists = await dbContext.InboxEvents.Find(e => e.EventId == envelope.EventId).AnyAsync();
            if (exists)
            {
                _logger.LogWarning("[Inbox] Event {EventId} previously processed. Deduplicating.", envelope.EventId);
                return;
            }

            try
            {
                // Register inbox event
                await dbContext.InboxEvents.InsertOneAsync(new InboxEvent
                {
                    EventId = envelope.EventId,
                    EventType = envelope.EventType,
                    ProcessedAt = DateTime.UtcNow
                });

                // Process specific notification outcomes
                var tenantId = Guid.Parse(envelope.TenantId);
                string message = BuildNotificationMessage(envelope);

                if (!string.IsNullOrEmpty(message))
                {
                    var notification = new Notification
                    {
                        Id = Guid.NewGuid(),
                        TenantId = tenantId,
                        EventType = envelope.EventType,
                        Message = message,
                        IsRead = false,
                        CreatedAt = DateTime.UtcNow
                    };

                    await dbContext.Notifications.InsertOneAsync(notification);

                    // Push real-time payload via SignalR
                    await _hubContext.Clients.Group(envelope.TenantId).SendAsync("ReceiveNotification", new
                    {
                        id = notification.Id,
                        eventType = notification.EventType,
                        message = notification.Message,
                        createdAt = notification.CreatedAt
                    });

                    _logger.LogInformation("[SignalR Broadcast] Pushed room broadcast to tenant={Tenant}", envelope.TenantId);
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error storing notification");
                throw;
            }
        }

        private string BuildNotificationMessage(EventEnvelope envelope)
        {
            try
            {
                return envelope.EventType switch
                {
                    "order.approved" => $"Đơn hàng {envelope.Data.GetProperty("orderId").GetString()} đã được phê duyệt thành công.",
                    "order.cancelled" => $"Đơn hàng {envelope.Data.GetProperty("orderId").GetString()} đã bị huỷ. Lý do: {envelope.Data.GetProperty("reason").GetString()}",
                    "payment.succeeded" => $"Thanh toán hóa đơn cho đơn hàng {envelope.Data.GetProperty("orderId").GetString()} đã được xác nhận thành công.",
                    "payment.failed" => $"Thanh toán hóa đơn cho đơn hàng {envelope.Data.GetProperty("orderId").GetString()} thất bại.",
                    "inventory.low_stock" => $"Sản phẩm {envelope.Data.GetProperty("sku").GetString()} đang ở mức tồn kho thấp (< {envelope.Data.GetProperty("threshold").GetInt32()}).",
                    "invoice.created" => $"Hoá đơn mới {envelope.Data.GetProperty("invoiceId").GetString()} cho đơn hàng {envelope.Data.GetProperty("orderId").GetString()} đã được xuất bản.",
                    _ => string.Empty
                };
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Could not map fields for notification type: {Type}", envelope.EventType);
                return $"Sự kiện {envelope.EventType} đã xảy ra.";
            }
        }
    }
}
