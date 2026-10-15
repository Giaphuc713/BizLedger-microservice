using MongoDB.Driver;
using notification_service;
using notification_service.Data;
using notification_service.Messaging;
using notification_service.Models;
using notification_service.Services;
using notification_service.Services.impl;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
// Add controllers & JSON formatting
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
    });
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Add MongoDbContext
var mongoConnectionString = builder.Configuration.GetConnectionString("MongoConnection") 
    ?? throw new InvalidOperationException("Mongo connection string not found.");
builder.Services.AddSingleton(new MongoDbContext(mongoConnectionString, "notification_db"));

//Add Services
builder.Services.AddScoped<INotificationService, NotificationService>();

builder.Services.AddSignalR();
// Add Kafka Consumer background worker
builder.Services.AddHostedService<KafkaConsumerService>();

// CORS configuration (allow dynamic requests from gateway/frontends)
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyHeader()
              .AllowAnyMethod()
              .SetIsOriginAllowed(_ => true) // Local development hub socket wildcard
              .AllowCredentials();
    });
});

var app = builder.Build();

// Ensure MongoDB Indexes are created on Startup
using (var scope = app.Services.CreateScope())
{
    try
    {
        var db = scope.ServiceProvider.GetRequiredService<MongoDbContext>();

        // Create unique index on InboxEvent.EventId
        var indexKeys = Builders<InboxEvent>.IndexKeys.Ascending(e => e.EventId);
        var indexModel = new CreateIndexModel<InboxEvent>(indexKeys, new CreateIndexOptions { Unique = true });
        db.InboxEvents.Indexes.CreateOne(indexModel);

        // Create index on Notification.TenantId and compound Index.IsRead
        var tenantKey = Builders<Notification>.IndexKeys.Ascending(n => n.TenantId);
        db.Notifications.Indexes.CreateOne(new CreateIndexModel<Notification>(tenantKey));

        var tenantReadKeys = Builders<Notification>.IndexKeys.Combine(
            Builders<Notification>.IndexKeys.Ascending(n => n.TenantId),
            Builders<Notification>.IndexKeys.Ascending(n => n.IsRead)
        );
        db.Notifications.Indexes.CreateOne(new CreateIndexModel<Notification>(tenantReadKeys));

        Console.WriteLine("[DB Bootstrap] MongoDB notification databases indexes initialized successfully.");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"[DB Bootstrap] Failed to initialize Mongo indexes: {ex.Message}");
    }
}
app.UseCors();

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseAuthorization();

// Map SignalR Hub
app.MapHub<NotificationHub>("/hubs/notifications");

app.MapControllers();

app.Run();
