package domain;

public class PrimaryKEY {
    private String pkAttribute;

    public PrimaryKEY() {
    }

    public PrimaryKEY(String pkAttribute) {
        this.pkAttribute = pkAttribute;
    }
    public String getPkAttribute() {
        return pkAttribute;
    }
    public void setPkAttribute(String pkAttribute) {
        this.pkAttribute = pkAttribute;
    }
}

