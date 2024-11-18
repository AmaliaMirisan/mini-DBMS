package utils;

import domain.DBMS;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.File;

public class Utils {
    private static final String DBMS_XML_FILE = "catalog.xml";
    public static DBMS loadDBMSFromXML() {
        File xmlFile = new File(DBMS_XML_FILE);
        if (xmlFile.exists()) {
            try {
                JAXBContext context = JAXBContext.newInstance(DBMS.class);
                Unmarshaller unmarshaller = context.createUnmarshaller();
                return (DBMS) unmarshaller.unmarshal(xmlFile);
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void saveDBMSToXML(DBMS dbms) {
        try {
            File xmlFile = new File(DBMS_XML_FILE);
            JAXBContext context = JAXBContext.newInstance(DBMS.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(dbms, xmlFile);
            System.out.println("DBMS structure saved to " + DBMS_XML_FILE);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public static boolean validateValueType(String columnType, String value) {
        try {
            switch (columnType.toUpperCase()) {
                case "INT":
                    Integer.parseInt(value);
                    break;
                case "STRING":
                    // No specific validation for STRING
                    break;
                default:
                    return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


}
