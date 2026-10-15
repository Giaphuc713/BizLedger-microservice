namespace notification_service.Models
{
    public class WebhookDeliveryLog
    {
        public Guid Id { get; set; } = Guid.NewGuid();

        public Guid SubscriptionId { get; set; }

        public string EventId { get; set; } = string.Empty;

        public string EventType { get; set; } = string.Empty;

        public string Payload { get; set; } = string.Empty;

        public int? HttpStatus { get; set; }

        public bool IsSuccess { get; set; }

        public string? ErrorMessage { get; set; }

        public int AttemptCount { get; set; }

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}
