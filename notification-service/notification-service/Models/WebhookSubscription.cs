namespace notification_service.Models
{
    public class WebhookSubscription
    {
        public Guid Id { get; set; } = Guid.NewGuid();

        public Guid TenantId { get; set; }

        public string WebhookUrl { get; set; } = string.Empty;

        public string Secret { get; set; } = string.Empty;

        public string EventTypes { get; set; } = string.Empty;

        public bool IsActive { get; set; } = true;

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}
