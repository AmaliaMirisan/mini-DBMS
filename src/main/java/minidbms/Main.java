package minidbms;

import ch.qos.logback.core.joran.spi.XMLUtil;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import domain.*;
import jdk.jshell.execution.Util;
import utils.Utils;

import javax.swing.text.Document;
import java.util.*;

import static utils.Utils.validateValueType;

public class Main {
    private static DBMS myDBMS = new DBMS();
    private static MongoClient mongoClient;
    private static final String CONNECTION_STRING = "mongodb+srv://amaliamirisan:amaliamirisan@minidbms.gk207.mongodb.net/?retryWrites=true&w=majority&appName=miniDBMS";

    private static Database currentDatabase = null;

    public static void initialize() {
        // Initialize MongoDB client
        mongoClient = MongoClients.create(CONNECTION_STRING);
        myDBMS = Utils.loadDBMSFromXML();
    }

    public static String processCommand(String command) {
        if (command.toLowerCase().startsWith("create database")) {
            String dbName = command.split(" ")[2];
            return createDatabase(dbName);
        } else if (command.toLowerCase().startsWith("drop database")){
            String dbName = command.split(" ")[2];
            return dropDatabase(dbName);
        }
        else if (command.toLowerCase().startsWith("use")){
            String dbName = command.split(" ")[1];
            return useDatabase(dbName);
        }
        else if (command.toLowerCase().startsWith("create table")){
            return handleCreateTable(command);
        }
        else if (command.toLowerCase().startsWith("drop table")){
            String tableName = command.split(" ")[2];
            return dropTable(tableName);
        }
        else if (command.toLowerCase().startsWith("create index")){
            return createIndex(command);
        }
        else if (command.toLowerCase().startsWith("insert")){
            return handleInsertAsKeyValue(command);
        }
        else if (command.toLowerCase().startsWith("delete")){
            return handleDelete(command);
        }
        else if ("exit".equalsIgnoreCase(command)) {
            return "Exiting...";
        } else {
            return "Unknown command.";
        }
    }
    private static String createDatabase(String dbName) {
        // Check if the database already exists
        if (myDBMS.getDatabaseByName(dbName) != null) {
            return "Database " + dbName + " already exists!";
        }

        // Create the database in MongoDB
        MongoDatabase database = mongoClient.getDatabase(dbName);
        System.out.println("MongoDB database '" + dbName + "' created.");

        // Update in-memory DBMS structure and XML catalog
        myDBMS.createDatabase(dbName);
        Utils.saveDBMSToXML(myDBMS);
        return "Database '" + dbName + "' created successfully.";
    }

    private static String dropDatabase(String dbName){
        // check if the database exists
        if(myDBMS.getDatabaseByName(dbName) == null){
            return "Database '" + dbName + "' does not exist!";
        }

        // drop the database in MongoDB
        try{
            mongoClient.getDatabase(dbName).drop();
            System.out.println("MongoDB database '" + dbName + "' dropped successfully!");
        }catch(Exception e){
            e.printStackTrace();
            return "Failed to drop MongoDB database '" + dbName + "'";
        }

        // update in-memory DBMS structure and XML catalog
        myDBMS.dropDatabase(dbName);
        Utils.saveDBMSToXML(myDBMS);

        return "Database '" + dbName + "' dropped successfully!";
    }
    private static String useDatabase(String dbName) {
        Database db = myDBMS.getDatabaseByName(dbName);
        // check if the database exists
        if(db == null){
            return "Database '" + dbName + "' does not exist!";
        }

        // set the current database
        currentDatabase = db;
        return "Using database '" + dbName + "'";


    }
    public static String createTable(String tableName, List<Column> columns, List<PrimaryKEY> primaryKeys, List<ForeignKEY> foreignKeys) {
        if (currentDatabase == null){
            return "No database selected! Please select a database first.";
        }

        //check if table already exists in the current database
        if (currentDatabase.getTableByName(tableName) != null){
            return "Table " + tableName + " already exists in database " + currentDatabase.getDatabaseName() + ".";
        }

        //ensure at least one primary key is defined
        if(primaryKeys == null || primaryKeys.isEmpty()) {
            return "Table '" + tableName + "' must have a primary key!";
        }

        //validate that referenced tables for foreign keys exists in the current database
        if (foreignKeys != null){
            for (ForeignKEY foreignKey : foreignKeys){
                String referencedTable = foreignKey.getRefTable();
                if (currentDatabase.getTableByName(referencedTable) == null){
                    return "Foreign key references table " + referencedTable + ", which does not exist in the current database.";
                }
            }
        }
        //create and add the new table
        Table newTable = new Table(tableName, columns, primaryKeys, foreignKeys);
        currentDatabase.createTable(newTable);

        //update catalog.xml
        Utils.saveDBMSToXML(myDBMS);

        // Update MongoDB structure
        MongoDatabase database = mongoClient.getDatabase(currentDatabase.getDatabaseName());

        // Check if collection (table) already exists
        boolean collectionExists = database.listCollectionNames()
                .into(new ArrayList<>()).contains(tableName);

        if (!collectionExists) {
            database.createCollection(tableName);
            System.out.println("MongoDB collection '" + tableName + "' created in database " + currentDatabase.getDatabaseName());
        } else {
            System.out.println("MongoDB collection '" + tableName + "' already exists in database " + currentDatabase.getDatabaseName());
        }

        return "Table " + tableName + " created successfully in database " + currentDatabase.getDatabaseName() + ".";


    }
    //method to parse and execute create table
    /*private static String handleCreateTable(String command) {
        try {
            String[] parts = command.split(" ", 4); // ["create", "table", "tableName", "(column1 TYPE, ...)"]
            String tableName = parts[2];
            String columnsPart = parts[3];

            //parse columns, primary key, foreign keys
            List<Column> columns = new ArrayList<>();
            List<PrimaryKEY> primaryKeys = new ArrayList<>();
            List<ForeignKEY> foreignKeys = new ArrayList<>();

            columnsPart = columnsPart.substring(1, columnsPart.length() - 1); // Remove parentheses
            String[] columnDefs = columnsPart.split(",\\s*");

            for (String def : columnDefs) {
                if (def.toLowerCase().startsWith("primary key")) {
                    // Primary key parsing logic
                    String pkColumns = def.split("\\(")[1].replace(")", "");
                    primaryKeys.add(new PrimaryKEY(pkColumns.trim()));
                } else if (def.toLowerCase().startsWith("foreign key")) {
                    // Foreign key parsing logic
                    String[] fkParts = def.split(" ");
                    String fkColumn = fkParts[2];
                    String referencedTable = fkParts[4].split("\\(")[0];
                    String referencedColumn = fkParts[4].split("\\(")[1].replace(")", "");
                    foreignKeys.add(new ForeignKEY(fkColumn, referencedTable, referencedColumn));
                } else {
                    // Regular column definition
                    String[] colParts = def.trim().split(" ");
                    String columnName = colParts[0];
                    String columnType = colParts[1];
                    columns.add(new Column(columnName, columnType));
                }
            }

            // Ensure primary key exists
            if (primaryKeys.isEmpty()) {
                return "Error: Table must have a primary key.";
            }

            // Call the method to create the table
            return createTable(tableName, columns, primaryKeys, foreignKeys);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing create table command.";
        }
    }*/

    private static String handleCreateTable(String command) {
        try {
            String[] parts = command.split(" ", 4); // ["create", "table", "tableName", "(column1 TYPE, ...)"]
            String tableName = parts[2];
            String columnsPart = parts[3];

            // Initialize lists for columns, primary keys, and foreign keys
            List<Column> columns = new ArrayList<>();
            List<PrimaryKEY> primaryKeys = new ArrayList<>();
            List<ForeignKEY> foreignKeys = new ArrayList<>();

            // Extract PRIMARY KEY clause if present
            int pkIndex = columnsPart.toLowerCase().indexOf("primary key");
            if (pkIndex != -1) {
                // Locate the start and end of the PRIMARY KEY clause by finding the parentheses
                int startParen = columnsPart.indexOf("(", pkIndex);
                int endParen = columnsPart.indexOf(")", startParen);

                // Extract and parse PRIMARY KEY clause
                String pkClause = columnsPart.substring(pkIndex, endParen + 1).trim();
                columnsPart = columnsPart.substring(0, pkIndex) + columnsPart.substring(endParen + 1).trim(); // Remove PRIMARY KEY clause from columnsPart

                // Parse the columns in PRIMARY KEY (e.g., PRIMARY KEY (col1, col2))
                String pkColumns = pkClause.split("\\(", 2)[1].replace(")", "").trim();
                String[] pkColumnNames = pkColumns.split(",\\s*");
                for (String pkColumn : pkColumnNames) {
                    primaryKeys.add(new PrimaryKEY(pkColumn.trim()));
                }
            }



            // Extract FOREIGN KEY clauses if present and handle them separately
            while (columnsPart.toLowerCase().contains("foreign key")) {
                int fkIndex = columnsPart.toLowerCase().indexOf("foreign key");
                String fkClause = columnsPart.substring(fkIndex);
                columnsPart = columnsPart.substring(0, fkIndex).trim(); // Remove FOREIGN KEY clause from columnsPart

                String[] fkParts = fkClause.split(" ");
                if (fkParts.length >= 5 && fkParts[4].contains("(")) {
                    String fkColumn = fkParts[2];
                    String referencedTable = fkParts[4].split("\\(")[0];
                    String referencedColumn = fkParts[4].split("\\(")[1].replace(")", "");
                    fkColumn = fkColumn.replaceAll("[()]", "");
                    foreignKeys.add(new ForeignKEY(fkColumn, referencedTable, referencedColumn));
                } else {
                    return "Error: Invalid foreign key syntax.";
                }
            }

            // Remove outer parentheses from columnsPart and split by commas
            columnsPart = columnsPart.substring(1, columnsPart.length() - 1).trim(); // Remove parentheses
            String[] columnDefs = columnsPart.split(",\\s*");

            // Parse each column definition, ignoring PRIMARY KEY as it has been handled
            for (String def : columnDefs) {
                def = def.trim();
                // Parse regular column definition
                String[] colParts = def.split(" ");
                if (colParts.length == 2) {
                    String columnName = colParts[0];
                    String columnType = colParts[1];
                    columns.add(new Column(columnName, columnType));
                } else {
                    return "Error: Invalid column definition syntax.";
                }
            }

            // Ensure at least one primary key exists
            if (primaryKeys.isEmpty()) {
                return "Error: Table must have a primary key.";
            }

            // Call the method to create the table
            return createTable(tableName, columns, primaryKeys, foreignKeys);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing create table command.";
        }
    }

    private static String dropTable(String tableName) {
        // check if there is a selected database
        if (currentDatabase == null) {
            return "No database selected!";
        }

        // check is table exists in current database
        Table tableToDrop = currentDatabase.getTableByName(tableName);
        if (tableToDrop == null) {
            return "Table '" + tableName + "' does not exist in the current database!";
        }

        // check if table is referenced as foreign key in another tables
        for (Table table : currentDatabase.getTables()) {
            for (ForeignKEY fk : table.getForeignKeys()) {
                if (fk.getRefTable().equals(tableName)) {
                    return "Cannot drop table '" + tableName + "' as it is referenced by foreign key in table '" + table.getTableName() + "'";
                }
            }
        }

        // delete table from MongoDB
        try {
            mongoClient.getDatabase(currentDatabase.getDatabaseName()).getCollection(tableName).drop();
            System.out.println("MongoDB collection '" + tableName + "' dropped successfully from database '" + currentDatabase.getDatabaseName() + "'!");
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to drop MongoDB collection '" + tableName + "'";
        }

        // update DBMS structure in memory and XML catalog
        currentDatabase.dropTable(tableName);
        Utils.saveDBMSToXML(myDBMS);

        return "Table '" + tableName + "' dropped successfully!";
    }


    private static String createIndex(String command) {
        try {
            // Parse the command: ["create", "index", "indexName", "on", "tableName(column1, column2, ...)"]
            String[] parts = command.split(" ", 5);
            String indexName = parts[2];
            String tableAndColumns = parts[4];

            // Extract table name and columns
            int startParen = tableAndColumns.indexOf("(");
            int endParen = tableAndColumns.indexOf(")");
            if (startParen == -1 || endParen == -1) {
                return "Error: Invalid syntax. Specify columns in parentheses.";
            }

            String tableName = tableAndColumns.substring(0, startParen).trim();
            String columnsPart = tableAndColumns.substring(startParen + 1, endParen);
            String[] columnNames = columnsPart.split(",\\s*");

            // Check if a database is selected
            if (currentDatabase == null) {
                return "Error: No database selected.";
            }

            // Verify table existence
            Table table = currentDatabase.getTableByName(tableName);
            if (table == null) {
                return "Error: Table '" + tableName + "' does not exist in the selected database.";
            }

            // Verify that columns exist in the table and prepare the list of column names for the index
            List<String> indexColumnNames = new ArrayList<>();
            for (String colName : columnNames) {
                Column column = table.getColumnByName(colName.trim());
                if (column == null) {
                    return "Error: Column '" + colName + "' does not exist in table '" + tableName + "'.";
                }
                indexColumnNames.add(colName.trim());
            }

            // Check for existing index with the same name
            for (Index existingIndex : table.getIndexes()) {
                if (existingIndex.getIndexName().equalsIgnoreCase(indexName)) {
                    return "Error: An index with name '" + indexName + "' already exists on table '" + tableName + "'.";
                }
            }

            // Check if 'unique' is present in the command
            boolean isUnique = command.toLowerCase().contains(" unique");

            // Create the Index object
            Index newIndex = new Index(indexName, tableName, indexColumnNames, isUnique);

            // Add the index to the table
            table.createIndex(newIndex);

            // Save changes to XML
            Utils.saveDBMSToXML(myDBMS);

            return "Index '" + indexName + "' created successfully on table '" + tableName + "'.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing create index command.";
        }
    }
    private static String handleInsertAsKeyValue(String command) {
        try {
            // Parse the command: "insert into tableName (col1, col2, ...) values (val1, val2, ...)"
            String[] parts = command.split(" ", 4);
            if (parts.length < 4 || !parts[1].equalsIgnoreCase("into")) {
                return "Error: Invalid syntax for insert command.";
            }

            String tableName = parts[2];
            String columnsAndValues = parts[3].trim();

            // Extract column names and values part
            int valuesIndex = columnsAndValues.toLowerCase().indexOf("values");
            if (valuesIndex == -1) {
                return "Error: Invalid syntax. Missing 'values' keyword.";
            }

            String columnsPart = columnsAndValues.substring(0, valuesIndex).trim();
            String valuesPart = columnsAndValues.substring(valuesIndex + "values".length()).trim();

            // Ensure both parts are wrapped in parentheses
            if (!columnsPart.startsWith("(") || !columnsPart.endsWith(")")) {
                return "Error: Invalid syntax. Columns must be enclosed in parentheses.";
            }
            if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")) {
                return "Error: Invalid syntax. Values must be enclosed in parentheses.";
            }

            // Remove parentheses and split the columns and values
            String[] columnNames = columnsPart.substring(1, columnsPart.length() - 1).split(",\\s*");
            String[] values = valuesPart.substring(1, valuesPart.length() - 1).split(",\\s*");

            // Check if a database is selected
            if (currentDatabase == null) {
                return "Error: No database selected.";
            }

            // Verify table existence
            Table table = currentDatabase.getTableByName(tableName);
            if (table == null) {
                return "Error: Table '" + tableName + "' does not exist in the selected database.";
            }

            // Validate column count and prepare data for insertion
            if (columnNames.length != values.length) {
                return "Error: Column count does not match value count.";
            }

            // Map column names to values
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < columnNames.length; i++) {
                String columnName = columnNames[i].trim();
                String value = values[i].trim().replace("'", ""); // Remove quotes from values

                // Verify column existence
                Column column = table.getColumnByName(columnName);
                if (column == null) {
                    return "Error: Column '" + columnName + "' does not exist in table '" + tableName + "'.";
                }

                row.put(columnName, value);
            }

            // Generate 'id' based on primary keys
            StringBuilder idBuilder = new StringBuilder();
            for (PrimaryKEY pk : table.getPrimaryKeys()) {
                String pkColumn = pk.getPkAttribute();
                if (!row.containsKey(pkColumn)) {
                    return "Error: Primary key column '" + pkColumn + "' is missing in the values.";
                }
                if (idBuilder.length() > 0) idBuilder.append("#");
                idBuilder.append(row.get(pkColumn));
            }
            String id = idBuilder.toString();

            // Generate 'value' by concatenating non-primary key values with '#'
            StringBuilder valueBuilder = new StringBuilder();
            for (String col : row.keySet()) {
                boolean isPrimaryKey = table.getPrimaryKeys().stream()
                        .anyMatch(pk -> pk.getPkAttribute().equals(col));
                if (!isPrimaryKey) {
                    if (valueBuilder.length() > 0) valueBuilder.append("#");
                    valueBuilder.append(row.get(col));
                }
            }
            String value = valueBuilder.toString();

            // Insert into MongoDB
            insertKeyValueIntoMongoDB(tableName, id, value);

            // Save changes to XML
            Utils.saveDBMSToXML(myDBMS);

            return "Record inserted successfully into table '" + tableName + "' as key-value pair.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing insert command.";
        }
    }

    // Method to insert into MongoDB with key-value format
    private static void insertKeyValueIntoMongoDB(String tableName, String id, String value) {
        MongoCollection<org.bson.Document> collection = mongoClient
                .getDatabase(currentDatabase.getDatabaseName())
                .getCollection(tableName);

        org.bson.Document document = new org.bson.Document();
        document.append("_id", id);
        document.append("value", value);
        collection.insertOne(document);

        System.out.println("Inserted key-value record into MongoDB collection '" + tableName + "' with id '" + id + "' and value '" + value + "'.");
    }
    private static String handleDelete(String command) {
        try {
            // parse the command: "delete from tableName where col1=val1 and col2=val2 ..."
            String[] parts = command.split(" ", 4);
            if (parts.length < 4 || !parts[1].equalsIgnoreCase("from") || !parts[3].startsWith("where")) {
                return "Error: Invalid syntax for delete command.";
            }

            String tableName = parts[2];
            String conditionPart = parts[3].substring("where".length()).trim();

            // parse conditions and construct primary key "id"
            String[] conditions = conditionPart.split("and");
            Map<String, String> conditionMap = new HashMap<>();
            for (String cond : conditions) {
                String[] keyValue = cond.split("=");
                if (keyValue.length != 2) {
                    return "Error: Invalid syntax. Expected 'column=value' format in condition.";
                }
                String column = keyValue[0].trim();
                String value = keyValue[1].trim().replace("'", ""); // Elimină ghilimelele
                conditionMap.put(column, value);
            }

            // verify if there is a database selected
            if (currentDatabase == null) {
                return "Error: No database selected.";
            }

            // verify if table exists
            Table table = currentDatabase.getTableByName(tableName);
            if (table == null) {
                return "Error: Table '" + tableName + "' does not exist in the selected database.";
            }

            // builds `id` using values from primary key
            StringBuilder idBuilder = new StringBuilder();
            for (PrimaryKEY pk : table.getPrimaryKeys()) {
                String pkColumn = pk.getPkAttribute();
                if (!conditionMap.containsKey(pkColumn)) {
                    return "Error: Missing primary key column '" + pkColumn + "' in conditions.";
                }
                if (idBuilder.length() > 0) idBuilder.append("#");
                idBuilder.append(conditionMap.get(pkColumn));
            }
            String id = idBuilder.toString();

            // verify if record exists in MongoDB using "id"
            if (!recordExistsInMongoDB(tableName, id)) {
                return "Error: No record found with primary key '" + id + "' in table '" + tableName + "'.";
            }

            // delete record from MongoDB considering `_id`
            deleteByIdFromMongoDB(tableName, id);

            // save changes in XML
            Utils.saveDBMSToXML(myDBMS);

            return "Record deleted successfully from table '" + tableName + "' where primary key is '" + id + "'.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing delete command.";
        }
    }
    // method for deleting a record from MongoDB using '_id' as a filter
    private static void deleteByIdFromMongoDB(String tableName, String id) {
        MongoCollection<org.bson.Document> collection = mongoClient
                .getDatabase(currentDatabase.getDatabaseName())
                .getCollection(tableName);

        // create a filter for '_id' and delete document
        org.bson.Document filter = new org.bson.Document("_id", id);
        collection.deleteOne(filter);

        System.out.println("Deleted record from MongoDB collection '" + tableName + "' with _id = '" + id + "'.");
    }

    // method for verifying the existence of the record in MongoDB considering '_id'
    private static boolean recordExistsInMongoDB(String tableName, String id) {
        MongoCollection<org.bson.Document> collection = mongoClient
                .getDatabase(currentDatabase.getDatabaseName())
                .getCollection(tableName);

        // create a filter using primary key '_id'
        org.bson.Document filter = new org.bson.Document("_id", id);
        return collection.countDocuments(filter) > 0;
    }


    public static void shutdown() {
        mongoClient.close();
    }
}

//---------------------------CLASIC INSERT & DELETE---------------------------//
/*private static String handleInsert(String command) {
        try {
            // Parse the command: "insert into tableName (col1, col2, ...) values (val1, val2, ...)"
            String[] parts = command.split(" ", 4);
            if (parts.length < 4 || !parts[1].equalsIgnoreCase("into")) {
                return "Error: Invalid syntax for insert command.";
            }

            String tableName = parts[2];
            String columnsAndValues = parts[3].trim();

            // Extract column names and values part
            int valuesIndex = columnsAndValues.toLowerCase().indexOf("values");
            if (valuesIndex == -1) {
                return "Error: Invalid syntax. Missing 'values' keyword.";
            }

            String columnsPart = columnsAndValues.substring(0, valuesIndex).trim();
            String valuesPart = columnsAndValues.substring(valuesIndex + "values".length()).trim();

            // Ensure both parts are wrapped in parentheses
            if (!columnsPart.startsWith("(") || !columnsPart.endsWith(")")) {
                return "Error: Invalid syntax. Columns must be enclosed in parentheses.";
            }
            if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")) {
                return "Error: Invalid syntax. Values must be enclosed in parentheses.";
            }

            // Remove parentheses and split the columns and values
            String[] columnNames = columnsPart.substring(1, columnsPart.length() - 1).split(",\\s*");
            String[] values = valuesPart.substring(1, valuesPart.length() - 1).split(",\\s*");

            // Check if a database is selected
            if (currentDatabase == null) {
                return "Error: No database selected.";
            }

            // Verify table existence
            Table table = currentDatabase.getTableByName(tableName);
            if (table == null) {
                return "Error: Table '" + tableName + "' does not exist in the selected database.";
            }

            // Validate column count and prepare data for insertion
            if (columnNames.length != values.length) {
                return "Error: Column count does not match value count.";
            }

            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < columnNames.length; i++) {
                String columnName = columnNames[i].trim();
                String value = values[i].trim().replace("'", ""); // Remove quotes from values

                // Verify column existence
                Column column = table.getColumnByName(columnName);
                if (column == null) {
                    return "Error: Column '" + columnName + "' does not exist in table '" + tableName + "'.";
                }

                // Validate data type
                if (!validateValueType(column.getType(), value)) {
                    return "Error: Invalid data type for column '" + columnName + "'. Expected " + column.getType() + ".";
                }

                row.put(columnName, value);
            }

            // Insert row into MongoDB
            insertIntoMongoDB(tableName, row);

            // Save changes to XML
            Utils.saveDBMSToXML(myDBMS);

            return "Record inserted successfully into table '" + tableName + "'.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing insert command.";
        }
    }

    private static String handleDelete(String command) {
        try {
            // Parse the command: "delete from tableName where columnName=value"
            String[] parts = command.split(" ", 4);
            if (parts.length < 4 || !parts[1].equalsIgnoreCase("from") || !parts[3].startsWith("where")) {
                return "Error: Invalid syntax for delete command.";
            }

            String tableName = parts[2];
            String conditionPart = parts[3].substring("where".length()).trim();

            // Extract column name and value for the WHERE condition
            String[] condition = conditionPart.split("=");
            if (condition.length != 2) {
                return "Error: Invalid syntax. Expected 'columnName=value' format for condition.";
            }

            String columnName = condition[0].trim();
            String value = condition[1].trim().replace("'", ""); // Remove quotes around the value

            // Check if a database is selected
            if (currentDatabase == null) {
                return "Error: No database selected.";
            }

            // Verify table existence
            Table table = currentDatabase.getTableByName(tableName);
            if (table == null) {
                return "Error: Table '" + tableName + "' does not exist in the selected database.";
            }

            // Verify that the specified column exists in the table
            Column column = table.getColumnByName(columnName);
            if (column == null) {
                return "Error: Column '" + columnName + "' does not exist in table '" + tableName + "'.";
            }

            // Delete from MongoDB
            deleteFromMongoDB(tableName, columnName, value);

            // Save changes to XML
            Utils.saveDBMSToXML(myDBMS);

            return "Record deleted successfully from table '" + tableName + "' where " + columnName + " = '" + value + "'.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing delete command.";
        }
    }
    // Method to insert into MongoDB
    private static void insertIntoMongoDB(String tableName, Map<String, String> row) {
        MongoCollection<org.bson.Document> collection = mongoClient
                .getDatabase(currentDatabase.getDatabaseName())
                .getCollection(tableName);

        org.bson.Document document = new org.bson.Document();
        row.forEach(document::append);
        collection.insertOne(document);

        System.out.println("Inserted record into MongoDB collection '" + tableName + "' in database '" + currentDatabase.getDatabaseName() + "'");
    }
    // Method to delete from MongoDB
    private static void deleteFromMongoDB(String tableName, String columnName, String value) {
        MongoCollection<org.bson.Document> collection = mongoClient
                .getDatabase(currentDatabase.getDatabaseName())
                .getCollection(tableName);

        // Create a filter based on the column and value for the delete operation
        org.bson.Document filter = new org.bson.Document(columnName, value);
        collection.deleteOne(filter);

        System.out.println("Deleted record from MongoDB collection '" + tableName + "' in database '" + currentDatabase.getDatabaseName() + "' where " + columnName + " = '" + value + "'");
    }
*/
