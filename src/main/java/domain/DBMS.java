package domain;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "databases")
@XmlType(propOrder = { "databases" })
public
class DBMS {
    @XmlElement(name = "database")
    private List<Database> databases;

    public DBMS() {
        this.databases = new ArrayList<>();
    }

    public List<Database> getDatabasesFunction() {
        return databases;
    }

    public void setDatabases(List<Database> databases) {
        this.databases = databases;
    }

    public void createDatabase(String name) {
        Database newDatabase = new Database(name);
        databases.add(newDatabase);
    }

    public void addDatabase(Database newDatabase) {
        databases.add(newDatabase);
    }

    public void dropDatabase(String name) {
        Database databaseToRemove = null;
        for (Database database : databases) {
            if (database.getDatabaseName().equalsIgnoreCase(name)) {
                databaseToRemove = database;
                break;
            }
        }
        if (databaseToRemove != null) {
            databases.remove(databaseToRemove);
        }
    }

    public List<Database> listDatabases() {
        return databases;
    }

    public Database getDatabaseByName(String name){
        for (Database database: this.databases) {
            if (database.getDatabaseName().equalsIgnoreCase(name)) {
                return database;
            }
        }
        return null;
    }
}
