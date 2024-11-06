package domain;
import javax.xml.bind.annotation.*;
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "column")
@XmlType(propOrder = { "columnName", "type" ,"length"})
public class Column {
    @XmlAttribute
    private String columnName;
    @XmlAttribute
    private String type;
    @XmlAttribute
    private Integer length;
    @XmlAttribute
    private Boolean unique;

    public Column(){

    }
    public Column(String name, String type){
        this.columnName = name;
        this.type = type;
        this.unique = null;
    }
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Boolean getUnique() {
        return unique;
    }

    public void setUnique(Boolean unique) {
        this.unique = unique;
    }
}
