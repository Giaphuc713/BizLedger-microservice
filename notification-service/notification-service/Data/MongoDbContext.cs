using MongoDB.Driver;
using notification_service.Models;

namespace notification_service.Data
{
    public class MongoDbContext
    {
        private IMongoDatabase _database;

        public MongoDbContext(string connectionString, string databaseName)
        {
            var client = new MongoClient(connectionString);
            _database = client.GetDatabase(databaseName);
        }
        public IMongoCollection<Notification> Notifications => _database.GetCollection<Notification>("notifications");
        public IMongoCollection<InboxEvent> InboxEvents => _database.GetCollection<InboxEvent>("inbox_events");
        public IMongoCollection<WebhookSubscription> WebhookSubscriptions => _database.GetCollection<WebhookSubscription>("webhook_subscriptions");
        public IMongoCollection<WebhookDeliveryLog> WebhookDeliveryLogs => _database.GetCollection<WebhookDeliveryLog>("webhook_delivery_logs");
    }
}
