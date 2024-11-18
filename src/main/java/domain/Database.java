package domain;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD )
@XmlRootElement(name = "database")
@XmlType(propOrder = { "databaseName", "tables" })
public
class Database {
    @XmlAttribute
    private String databaseName;
    @XmlElement(name = "table")
    @XmlElementWrapper(name = "tables")
    private List<Table> tables;

    public Database() {
    }

    public Database(String name) {
        this.databaseName = name;
        this.tables = new ArrayList<>();
    }

    public void createTable(Table newTable) {
        tables.add(newTable);
    }

    public void dropTable(String tableName) {
        Table tableToRemove = null;
        for (Table table : tables) {
            if (table.getTableName().equalsIgnoreCase(tableName)) {
                tableToRemove = table;
                break;
            }
        }
        if (tableToRemove != null) {
            tables.remove(tableToRemove);
        }
    }

    public List<Table> listTables() {
        return tables;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public Table getTableByName(String name) {
        for (Table table: this.tables) {
            if (table.getTableName().equalsIgnoreCase(name)) {
                return table;
            }
        }
        return null;
    }

    public List<Table> getTablesReferencing(String referencedTableName) {
        List<Table> referencingTables = new ArrayList<>();
        for (Table table : tables) {
            for (ForeignKEY fk : table.getForeignKeys()) {
                if (fk.getRefTable().equalsIgnoreCase(referencedTableName)) {
                    referencingTables.add(table);
                    break;
                }
            }
        }
        return referencingTables;
    }
}
