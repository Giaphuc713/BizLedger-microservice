using MongoDB.Driver;
using notification_service.Data;
using notification_service.Models;

namespace notification_service.Services.impl
{
    public class NotificationService : INotificationService
    {
        private readonly MongoDbContext _dbContext;

        public NotificationService(MongoDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public async Task<List<Notification>> GetNotificationsAsync(Guid tenantId)
        {
            return await _dbContext.Notifications
                .Find(n => n.TenantId == tenantId)
                .SortByDescending(n => n.CreatedAt)
                .Limit(50)
                .ToListAsync();
        }

        public async Task<bool> MarkAsReadAsync(Guid id, Guid tenantId)
        {
            var result = await _dbContext.Notifications.UpdateOneAsync(
                n => n.Id == id && n.TenantId == tenantId,
                Builders<Notification>.Update.Set(n => n.IsRead, true)
            );
            return result.ModifiedCount > 0;
        }

        public async Task MarkAllAsReadAsync(Guid tenantId)
        {
            await _dbContext.Notifications.UpdateManyAsync(
                n => n.TenantId == tenantId && !n.IsRead,
                Builders<Notification>.Update.Set(n => n.IsRead, true)
            );
        }

        public async Task<List<WebhookSubscription>> GetWebhooksAsync(Guid tenantId)
        {
            return await _dbContext.WebhookSubscriptions
                .Find(s => s.TenantId == tenantId)
                .ToListAsync();
        }

        public async Task<WebhookSubscription> CreateWebhookAsync(Guid tenantId, WebhookSubscription request)
        {
            request.TenantId = tenantId;
            request.Id = Guid.NewGuid();
            request.CreatedAt = DateTime.UtcNow;

            await _dbContext.WebhookSubscriptions.InsertOneAsync(request);
            return request;
        }

        public async Task<bool> DeleteWebhookAsync(Guid id, Guid tenantId)
        {
            var result = await _dbContext.WebhookSubscriptions.DeleteOneAsync(
                s => s.Id == id && s.TenantId == tenantId
            );
            return result.DeletedCount > 0;
        }

        public async Task<List<WebhookDeliveryLog>> GetDeliveriesAsync(Guid tenantId)
        {
            var subs = await _dbContext.WebhookSubscriptions
                .Find(s => s.TenantId == tenantId)
                .Project(s => s.Id)
                .ToListAsync();

            if (!subs.Any())
            {
                return new List<WebhookDeliveryLog>();
            }

            return await _dbContext.WebhookDeliveryLogs
                .Find(l => subs.Contains(l.SubscriptionId))
                .SortByDescending(l => l.CreatedAt)
                .Limit(50)
                .ToListAsync();
        }

        public async Task<Dictionary<string, object>> GetDashboardMetricsAsync(Guid tenantId)
        {
            var totalNotifications = await _dbContext.Notifications.CountDocumentsAsync(n => n.TenantId == tenantId);
            var unreadCount = await _dbContext.Notifications.CountDocumentsAsync(n => n.TenantId == tenantId && !n.IsRead);

            var subs = await _dbContext.WebhookSubscriptions
                .Find(s => s.TenantId == tenantId)
                .Project(s => s.Id)
                .ToListAsync();

            long totalDeliveries = 0;
            long successDeliveries = 0;

            if (subs.Any())
            {
                totalDeliveries = await _dbContext.WebhookDeliveryLogs.CountDocumentsAsync(l => subs.Contains(l.SubscriptionId));
                successDeliveries = await _dbContext.WebhookDeliveryLogs.CountDocumentsAsync(l => subs.Contains(l.SubscriptionId) && l.IsSuccess);
            }

            double successRate = totalDeliveries > 0 ? (double)successDeliveries / totalDeliveries * 100.0 : 100.0;

            return new Dictionary<string, object>
            {
                { "totalNotifications", totalNotifications },
                { "unreadNotifications", unreadCount },
                { "readNotifications", totalNotifications - unreadCount },
                { "totalWebhooks", subs.Count },
                { "totalDeliveries", totalDeliveries },
                { "webhookSuccessRate", Math.Round(successRate, 1) }
            };
        }
    }
}
