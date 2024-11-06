package domain;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "tables")
@XmlType(name = "tables", propOrder = {"tableName", "fileName", "columns", "primaryKeys", "foreignKeys", "indexes"})
public
class Table {
    @XmlAttribute
    private String tableName;
    @XmlAttribute
    private String fileName;
    @XmlElement(name = "attribute")
    @XmlElementWrapper(name = "structure")
    private List<Column> columns;
    @XmlElement(name = "pkAttribute")
    @XmlElementWrapper(name = "primaryKey")
    private List<PrimaryKEY> primaryKeys;
    @XmlElement(name = "foreignKey")
    @XmlElementWrapper(name = "foreignKeys")
    private List<ForeignKEY> foreignKeys;
    @XmlElement(name = "IndexAttribute")
    @XmlElementWrapper(name = "IndexAttributes")
    private List<Index> indexes;

    public Table() {
    }

    public Table(String name) {
        this.tableName = name;
        this.columns = new ArrayList<>();
        this.primaryKeys = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
        this.indexes = new ArrayList<>();
    }

    public Table(String tableName, List<Column> columns, List<PrimaryKEY> primaryKeys, List<ForeignKEY> foreignKeys) {
        this.tableName = tableName;
        this.fileName = tableName + ".bin";
        this.columns = columns;
        this.primaryKeys = primaryKeys;
        this.foreignKeys = foreignKeys;
        this.indexes = new ArrayList<>();
    }

    public void createIndex(Index newIndex) {
        indexes.add(newIndex);

    }

    public void dropIndex(String indexName) {
        Index indexToRemove = null;
        for (Index index : indexes) {
            if (index.getIndexName().equalsIgnoreCase(indexName)) {
                indexToRemove = index;
                break;
            }
        }
        if (indexToRemove != null) {
            indexes.remove(indexToRemove);
        }
    }

    public Column getColumnByName(String name) {
        for (Column column : this.columns) {
            if (column.getColumnName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        return null;
    }

    public List<Column> getColumnsNotPartOfPK() {
        List<Column> cols = new ArrayList<>();
        List<String> pkNames = primaryKeys.stream().map(PrimaryKEY::getPkAttribute).toList();
        for (Column col : columns) {
            if (!pkNames.contains(col.getColumnName())) {
                cols.add(col);
            }
        }
        return cols;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<PrimaryKEY> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<PrimaryKEY> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public List<ForeignKEY> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<ForeignKEY> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    public List<Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<Index> indexes) {
        this.indexes = indexes;
    }
    public Index getIndexByName(String indexName) {
        for (Index index : indexes) {
            if (index.getIndexName().equals(indexName)) {
                return index;
            }
        }
        return null;
    }

}