package domain;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "IndexAttribute")
@XmlType(name = "IndexAttribute", propOrder = {"indexName", "tableName", "columns", "isUnique"})
public class Index {
    @XmlElement
    private String indexName;
    @XmlElement
    private String tableName;

    // Wrapper for columns to ensure single <columns> tag in XML with nested elements
    @XmlElementWrapper(name = "columns")
    @XmlElement(name = "column")
    private List<String> columns;

    @XmlElement
    private boolean isUnique;

    public Index() {}

    public Index(String indexName, String tableName, List<String> columns, boolean isUnique) {
        this.indexName = indexName;
        this.tableName = tableName;
        this.columns = columns;
        this.isUnique = isUnique;
    }

    // Getter and Setter methods for the fields

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }
}
