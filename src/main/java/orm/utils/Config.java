package orm.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import orm.session.SessionFactory;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private int connectionPoolSize;
    private List<String> packages;

    private static Config instance;
    private String user;
    private String password;
    private String databaseUrl;
    private String databaseName;
    private boolean createSchemaOnStart;

    private Config() throws Exception {
        URL configUrl = getClass().getClassLoader().getResource("orm_config.json");
        if (configUrl == null){
            throw new Exception("orm_config.json not found");
        }
        Gson gson = new Gson();
        File configFile = new File(configUrl.toURI());
        Reader reader = new FileReader(configFile);
        JsonElement json = JsonParser.parseReader(reader);
        JsonElement packagesJson = json.getAsJsonObject().get("packages");
        packages = gson.fromJson(packagesJson, ArrayList.class);

        connectionPoolSize = json.getAsJsonObject().get("connection_pool_size").getAsInt();
        user = json.getAsJsonObject().get("user").getAsString();
        password = json.getAsJsonObject().get("password").getAsString();
        databaseUrl = json.getAsJsonObject().get("database_url").getAsString();
        databaseName = json.getAsJsonObject().get("database_name").getAsString();
        createSchemaOnStart = json.getAsJsonObject().get("create_schema_on_start").getAsBoolean();
        reader.close();
    }

    public static synchronized Config getInstance() throws Exception {
        // lazy-loading, double null-check, thread-safe Singleton
        if (instance == null){
            synchronized (SessionFactory.class){
                if (instance == null){
                    instance = new Config();
                }
            }
        }
        return instance;
    };

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public List<String> getPackages() {
        return packages;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public boolean isCreateSchemaOnStart() {
        return createSchemaOnStart;
    }
}
