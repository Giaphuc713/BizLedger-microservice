namespace notification_service.Models
{
    public class InboxEvent
    {
        public string Id { get; set; } = Guid.NewGuid().ToString();

        public string EventId { get; set; } = string.Empty;

        public string EventType { get; set; } = string.Empty;

        public DateTime ProcessedAt { get; set; } = DateTime.UtcNow;
    }
}
