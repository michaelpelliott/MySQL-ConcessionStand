import java.sql.*;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;


/**
 *  CS-HU 310, MySQL, Spring 2020
 *  This project simulates a concession stand inventory system.
 *  This project was co-authored by Michael Elliott and Cade Peterson
 */
public class Project {

    private static String USERNAME = "msandbox";
    private static String PASSWORD = "Ranger86*";
    private static int PORT_NUMBER = 55686;
    private static String TARGET = "127.0.0.1:" + PORT_NUMBER;
    private static String DB_NAME = "BasicInventorySystem";

    private static String CMD_START = "java Project";
    protected static String errorMsgFormat = "Error: %s %s\r\n";

    //Holds possible arg combinations for Project
    public enum UsageCmd{
        UsageCommand    (Procedures.printMenu,      null,       "Get Menu",                       new ArgType[]{ArgType.Skip},                                      "/?"),
        CreateItem      (Procedures.create,         "Item",     "Create an Item",                 new ArgType[]{ArgType.String, ArgType.String, ArgType.Decimal},   "<itemCode>", "<itemDescription>", "<price>"),
        CreatePurchase  (Procedures.create,         "Purchase", "Create a Purchase",              new ArgType[]{ArgType.String, ArgType.Number},                    "<itemCode>", "<PurchaseQuantity>"),
        CreateShipment  (Procedures.create,         "Shipment", "Create a Shipment",              new ArgType[]{ArgType.String, ArgType.Number, ArgType.Date},      "<itemCode>", "<ShipmentQuantity>", "<shipmentDate>"),
        GetItems        (Procedures.get,            "Item",     "Get a list of Items",            new ArgType[]{ArgType.StringOrAll},                               "<itemCode>"),
        GetShipments    (Procedures.get,            "Shipment", "Get a list of Shipments",        new ArgType[]{ArgType.StringOrAll},                               "<itemCode>"),
        GetPurchases    (Procedures.get,            "Purchase", "Get a list of Purchases",        new ArgType[]{ArgType.StringOrAll},                               "<itemCode>"),
        ItemsAvailable  (Procedures.itemsAvailable, null,       "Get a list of available Items",  new ArgType[]{ArgType.StringOrAll},                               "<itemCode>"),
        UpdateItem      (Procedures.updateItem,     "Item",     "Update an Item",                 new ArgType[]{ArgType.String, ArgType.Decimal},                   "<itemCode>", "<price>"),
        DeleteItem      (Procedures.delete,         "Item",     "Delete an Item",                 new ArgType[]{ArgType.String},                                    "<itemCode>"),
        DeleteShipment  (Procedures.delete,         "Shipment", "Delete a Shipment",              new ArgType[]{ArgType.String},                                    "<itemCode>"),
        DeletePurchase  (Procedures.delete,         "Purchase", "Delete a Purchase",              new ArgType[]{ArgType.String},                                    "<itemCode>"),
        None            (null, null, new String(), new ArgType[]{ArgType.Skip}, new String());

        private Procedures.Procedure procedure; //Lambda that will execute the code for the command
        private String description;             //Description of the command
        private ArgType[] argTypes;             //Types of args that lines up in the same order as reqArgs
        private String[] reqArgs;               //Required args
        private String tableName;               //Name of Table to use

        UsageCmd(Procedures.Procedure procedure, String tn, String description, ArgType[] at, String ... reqArgs){
            this.procedure = procedure;
            this.reqArgs = reqArgs;
            this.description = description;
            this.tableName = tn;
            this.argTypes = at;
        }

        String getTableName(){
            return tableName;
        }

        String getDescription(){
            return description;
        }

        Procedures.Procedure getProcedure(){
            return procedure;
        }

        String[] getReqArgs(){
            return reqArgs;
        }

        String getReqCmdString(){
            return getSpecArgs(this.getReqArgs());
        }

        public ArgType[] getArgTypes() {
            return argTypes;
        }

        /** CmdString helper method **/
        private String getSpecArgs(String[] params){
            String cmdString = CMD_START + " \"" + this.name() + "\" ";
            for(int i = 0; i < params.length; i++){
                cmdString += params[i] + " ";
            }
            return cmdString;
        }

        /** Similar to valueOf method **/
        static UsageCmd getValue(String name){
            UsageCmd[] cmds = UsageCmd.values();
            for(int i = 0; i < cmds.length; i++){
                String validName = cmds[i].name().toLowerCase();
                name = name.equals("/?") ? "usagecommand" : name.toLowerCase();
                if(validName.equals(name)){
                    return cmds[i];
                }
            }
            throw new IllegalArgumentException();
        }
    }

    /** Is able to determine if the string provided is a valid object of said type **/
    public enum ArgType{
        Number      ("Integer"),
        String      ("String (no '%' characters)"),
		StringOrAll	("String or % for all"),
        Decimal     ("Decimal(4,2)"),
        Date        ("Date (YYYY-MM-DD)"),
        Skip        (null);

        private String type;

        ArgType(String s){
            type = s;
        }

        public java.lang.String getType() {
            return type;
        }

        /** Determine object validity **/
        public boolean testObjectType(String s){
            char[] charArray = s.toCharArray();
            boolean b = false;
            switch (this){
                case StringOrAll:
                    if(s.equals("%")){
                        b = true;
                        break;
                    }else {
                        int i = 0;
                        while (i < charArray.length && charArray[i] != '%'){
                            i++;
                        }
                        b = i == charArray.length;
                        break;
                    }
                case Number:
                    try{
                        Integer.parseInt(s);
                        b = true;
                    }catch (NumberFormatException e){}
                    break;
                case String:
                    int i = 0;
                    while (i < charArray.length && charArray[i] != '%'){
                        i++;
                    }
                    b = i == charArray.length;
                    break;
                case Decimal:
                    Float f = 0f;
                    try{
                        f = Float.parseFloat(s);
                        s = f + "";
                    }catch (NumberFormatException e){}

                    if(f < 100 && f > -100){
                        int decIndex = s.indexOf('.');
                        if(!(decIndex > 2 || s.length() - decIndex > 3)){
                            b = true;
                        }
                    }
                    break;
                case Date:
                    try {
                        LocalDateTime.parse(s + "T00:00:00");
                        b = true;
                    }catch (DateTimeParseException e){}
                    break;
                default:
                    b = true;
            }
            return b;
        }

        /** Trim off trailing zeros **/
        public static String getFloat(String s){
            return Float.parseFloat(s) + "";
        }
    }

    //Holds error messages to be given to the user when incorrect args are provided
    protected enum ArgError{
        InvalidCmd          ("The command provided is not available."),
        TooFewArgs          ("Not Enough Args were provided for the command:"),
        TooManyArgs         ("Too many args were provided for the command:"),
        InvalidArgType      ("The arg provided was an invalid type:"),
        NoCommandProvided   ("No command was provided."),
        DataBaseError       ("DateBase Error"),
        CloseDataBase       ("Unable to Close:"),
        None                ("");

        private String msg;
        private ArrayList<Pair<Integer, String>> attachedMsgs;
        private String appendString;

        ArgError(String msg){
            this.msg = msg;
            attachedMsgs = new ArrayList<>();
        }

        String getMsg(){
            return msg;
        }

        void addAttachedMsg(int depth, String s){
            attachedMsgs.add(new Pair<Integer, String>(depth, s));
        }

        void setAppendString(String s){
            appendString = s;
        }

        void printError(){
            System.err.printf(errorMsgFormat, this.msg, (appendString != null ? appendString : ""));
            for(int i = 0; i < this.attachedMsgs.size(); i++){
                for(int j = 0; j < this.attachedMsgs.get(i).getA(); j++){
                    System.err.print("\t");
                }
                System.err.printf("-> %s\r\n", this.attachedMsgs.get(i).getB());
            }
        }
    }

    //////////////////////////
    // XXX Start of Project
    //////////////////////////

    private static ArgError error;          //Error to print if any
    private static UsageCmd cmd;            //Cmd to be execute
    private static String[] cmdArgs;        //Cmd args to be used for execute the cmd
    private static DataBase DB;             //DB being used to do queries

    /** Execute the command provided by the user **/
    public static void main(String[] args){
        error = ArgError.None;
        cmd = UsageCmd.None;

        //Establish the connection for all cmds except /?
        if(!(args.length == 0 || args[0].equals("/?"))) {
            try {
                DB = new DataBase();
            } catch (Exception e) {
                error = ArgError.DataBaseError;
                error.setAppendString("- Unable to connect the Database.");
                error.addAttachedMsg(1, e.toString());
            }
        }

        if(error != ArgError.None || !parseArgs(args)){
            error.printError();
        }else{
            cmd.getProcedure().execute(DB, cmdArgs);

            if(error != ArgError.None){
                error.printError();
            }
        }

        if(DB != null) {
            DB.closeDataBase();
        }

        if(error == ArgError.CloseDataBase){
            error.printError();
        }
    }

    /** Parses the args that the user provides to verify they are correct **/
    private static boolean parseArgs(String[] args){
        if(args.length == 0){
            error = ArgError.NoCommandProvided;
            error.addAttachedMsg(1, "Please refer to the \"Project Usage Commands\" for a list of possible commands.");
            error.addAttachedMsg(2, "java Project /?");
            return false;
        }else{
            //Get command
            try{
                cmd = UsageCmd.getValue(args[0]);

                //None isn't an option
                if(cmd == UsageCmd.None){
                    throw new IllegalArgumentException();
                }
            }catch (IllegalArgumentException e){
                error = ArgError.InvalidCmd;
                error.addAttachedMsg(1, "Please refer to the \"Project Usage Commands\" for a list of possible commands.");
                error.addAttachedMsg(2, "java Project /?");
                return false;
            }

            //Check number of args provided
            if(cmd != UsageCmd.CreateShipment) {
                if(args.length - (cmd == UsageCmd.UsageCommand ? 0 : 1) < cmd.getReqArgs().length){
                    error = ArgError.TooFewArgs;
                    int num = cmd.getReqArgs().length;
                    error.setAppendString(cmd.name());
                    error.addAttachedMsg(1, "At least " + num + " parameter" + (num > 1 ? "s are" : " is") + " used for this command.");
                    error.addAttachedMsg(2, cmd.getReqCmdString());
                    return false;
                }else if(args.length - (cmd != UsageCmd.UsageCommand ? 1 : 0) > cmd.getReqArgs().length){
                    error = ArgError.TooManyArgs;
                    int num = cmd.getReqArgs().length;
                    error.setAppendString(cmd.name());
                    error.addAttachedMsg(1, "At most " + num + " parameter" + (num > 1 ? "s are" : " is") + " used for this command.");
                    error.addAttachedMsg(2, cmd.getReqCmdString());
                    return false;
                }
            }
            else {
                /* 2 or 3 args is fine for CreateShipment */
                if(args.length < 3) {
                    error = ArgError.TooFewArgs;
                    int num = cmd.getReqArgs().length;
                    error.setAppendString(cmd.name());
                    error.addAttachedMsg(1, "At least " + num + " parameter" + (num > 1 ? "s are" : " is") + " used for this command.");
                    error.addAttachedMsg(2, cmd.getReqCmdString());
                    return false;
                }
                else if(args.length > 4){
                    error = ArgError.TooManyArgs;
                    int num = cmd.getReqArgs().length;
                    error.setAppendString(cmd.name());
                    error.addAttachedMsg(1, "At most " + num + " parameter" + (num > 1 ? "s are" : " is") + " used for this command.");
                    error.addAttachedMsg(2, cmd.getReqCmdString());
                    return false;
                }
            }
        }
         if( cmd != UsageCmd.CreateShipment) {
            for(int i = 1; i < args.length; i++){
                if(!cmd.argTypes[i - 1].testObjectType(args[i])){
                    error = ArgError.InvalidArgType;
                    error.setAppendString(cmd.argTypes[i - 1].getType());
                    error.addAttachedMsg(1, String.format("For Parameter(%d): %s", i - 1, cmd.getReqArgs()[i- 1]));
                    return false;
                }
            }
         }
         else {
             /* 2 args, and 3 args parse for Create Shipment */
             if ( args.length == 3 ) {
                 /* trying to pass in without a date, use current date for args[4] */
                 String[] argsTemp = new String[4];
                 for(int i = 0; i < args.length; i++) { 
                     /* args.length is 3, copy args[i] into argsTemp */
                     argsTemp[i] = args[i];
                 }
                 LocalDate today = LocalDate.now();
                 argsTemp[3] = today.toString();
                 args = argsTemp;
             } else {
                 /* do nothing I guess, all is well */
             }
                
            for(int i = 1; i < args.length; i++){
                if(!cmd.argTypes[i - 1].testObjectType(args[i])){
                    error = ArgError.InvalidArgType;
                    error.setAppendString(cmd.argTypes[i - 1].getType());
                    error.addAttachedMsg(1, String.format("For Parameter(%d): %s", i - 1, cmd.getReqArgs()[i- 1]));
                    return false;
                }
            }
         }

        //Copy command parameters into cmdArgs
        cmdArgs = new String[args.length];
        cmdArgs[0] = cmd.getTableName();
        for(int i = 1; i < args.length; i++){
            cmdArgs[i] = args[i];
        }

        return true;
    }

    /////////////////////////////////
    // XXX Start of lambda Functions
    /////////////////////////////////

    public static class Procedures {

        /** Prints Usage Menu **/
        private static void printMenu(String args[]){
            Project.UsageCmd[] cmds = Project.UsageCmd.values();
            System.out.println("\r\nProject Usage Commands");

            String[][] cmdExamples = new String[cmds.length - 1][3];
            int longestCmdStart = 0;
            int longestDescription = 0;
            int longestExample = 0;

            //Create Command Usage strings
            for(int i = 0; i <cmdExamples.length; i++){
                String[] cmdParts = cmds[i].getReqArgs();

                //Base usage example
                String cmdStart = CMD_START + " ";
                if(cmds[i] != Project.UsageCmd.UsageCommand){
                    cmdStart += cmds[i].name() + " ";
                }

                //Add command parts to usage example
                String cmdExample = "";
                for(int j = 0; j < cmdParts.length; j++){
                    cmdExample += cmdParts[j] + " ";
                }

                cmdExamples[i][0] = cmds[i].getDescription() + ": ";
                cmdExamples[i][1] = cmdStart;
                cmdExamples[i][2] = cmdExample;

                //Used to find the longest description for formatting
                if(cmdExamples[i][0].length() > longestDescription){
                    longestDescription = cmdExamples[i][0].length();
                }

                //Used to find the longest start string for formatting
                if(cmdExamples[i][1].length() > longestCmdStart){
                    longestCmdStart = cmdExamples[i][1].length();
                }
                //Used to find the longest example string for separating line
                if(cmdExamples[i][2].length() > longestExample){
                    longestExample = cmdExamples[i][2].length();
                }
            }

            //Make separating line between menu name and usage commands
            longestExample += longestDescription + longestCmdStart;
            String separatingLine1 = "";
            String separatingLine2 = "";
            for(int i = 0; i < longestExample; i++){
                separatingLine1 += "-";
                separatingLine2 += "=";
            }

            System.out.println(separatingLine1);

            //Print Headers for usage Commands
            String format1 = "%-" + (longestDescription - 1) + "s| %-" + (longestCmdStart - 2) + "s| %s\r\n";
            System.out.printf(format1, "Description", "Command", "Parameters");
            System.out.println(separatingLine2);

            //Print Command usage examples
            String format2 = "%-" + longestDescription + "s%-" + longestCmdStart + "s%s\r\n";
            for(int i = 0; i < cmdExamples.length; i++){
                System.out.printf(format2, cmdExamples[i][0], cmdExamples[i][1], cmdExamples[i][2]);
            }
        }
        protected static Procedure printMenu = (DataBase db, String[] args) -> printMenu(args);

        /** Creates an item for any given table **/
        private static void create(DataBase db, String[] args){
            try {
                int rs = -1; //Should never be -1 either it will throw an exception or it will be updated
                switch (args[0]) {
                    case "Item":
                        rs = db.updateStatement(String.format("call %s.CreateItem('%s', '%s', %s)", "%s", args[1], args[2], args[3]));
                        break;
                    case "Purchase":
                        rs = db.updateStatement(String.format("call %s.CreatePurchase('%s', %s)", "%s", args[1], args[2]));
                        break;
                    case "Shipment":
                        rs = db.updateStatement(String.format("call %s.CreateShipment('%s', %s, '%s')", "%s", args[1], args[2], args[3]));
					    break;
                }
                System.out.printf("Number of rows affected by the insert statement: %d\r\n", rs);
            }catch (SQLException e){
                error = ArgError.DataBaseError;
                error.setAppendString("Unable to create a " + args[0] + ".");
                error.addAttachedMsg(1, e.toString());
            }
        }
        protected static Procedure create = (DataBase db, String[] args) -> create(db, args);

        /** Gets a list of tuples from a table provided the item code **/
        private static void get(DataBase db, String[] args){
            try{
                ResultSet rs = null;
                if(!args[1].equals("%")) {
                    switch (args[0]) {
                        case "Item":
                            rs = db.executeStatement(String.format("call %s.GetItem('%s')", "%s", args[1]));
                            break;
                        case "Shipment":
                            rs = db.executeStatement(String.format("call %s.GetShipments('%s')", "%s", args[1]));
                            break;
                        case "Purchase":
                            rs = db.executeStatement(String.format("call %s.GetPurchases('%s')", "%s", args[1]));
                            break;
                    }
                }else{
                    rs = db.executeStatement(String.format("select * From %s.%s", "%s", args[0]));
                }
                printReturnResultSet(rs);
            }catch (SQLException e){
                error = ArgError.DataBaseError;
                error.setAppendString("- Unable to get " + args[0] +  " info.");
                error.addAttachedMsg(1, e.toString());
            }
        }
        protected static Procedure get = (DataBase db, String[] args) -> get(db, args);

        /** Updates an Item provide the price to update it **/
        private static void updateItem(DataBase db, String[] args){
            try{
                int rs = db.updateStatement(String.format("call %s.UpdateItem('%s', %s)", "%s", args[1], args[2]));
                System.out.printf("Number of rows affected by the update statement: %d\r\n", rs);
            }catch (SQLException e){
                error = ArgError.DataBaseError;
                error.setAppendString("- Unable to get update the item price for " + args[1] +  ".");
                error.addAttachedMsg(1, e.toString());
            }
        }
        protected static Procedure updateItem = (DataBase db, String[] args) -> updateItem(db, args);

        /** Get a list of items available **/
        private static void itemsAvailable(DataBase db, String[] args){
            try{
                ResultSet rs;
                if(args[1].equals("%")){
                    rs = db.executeStatement(String.format("call %s.ItemsAvailable('%s', 1)", "%s", 1)); //Second arg doesn't matter
                }else{
                    rs = db.executeStatement(String.format("call %s.ItemsAvailable('%s', 0)", "%s", args[1]));
                }
                printReturnResultSet(rs);
            }catch (SQLException e){
                error = ArgError.DataBaseError;
                error.setAppendString("- Unable to get Available Items.");
                error.addAttachedMsg(1, e.toString());
            }
        }
        protected static Procedure itemsAvailable = (DataBase db, String[] args) -> itemsAvailable(db, args);

        /** delete rows from the table provided **/
        private static void delete(DataBase db, String[] args){
            try{
                int rs = -1; //Should never be -1 either it will throw an exception or it will be updated
                switch (args[0]) {
                    case "Item":
                        rs = db.updateStatement(String.format("call %s.DeleteItem('%s')", "%s", args[1]));
                        break;
                    case "Shipment":
                        rs = db.updateStatement(String.format("call %s.DeleteShipment('%s')", "%s", args[1]));
                        break;
                    case "Purchase":
                        rs = db.updateStatement(String.format("call %s.DeletePurchase('%s')", "%s", args[1]));
                        break;
                }
                System.out.printf("Number of rows affected by the update statement: %d\r\n", rs);
            }catch (SQLException e){
                error = ArgError.DataBaseError;
                error.setAppendString(String.format("Unable to delete Item Code %s from %s.", args[1], args[0]));
                error.addAttachedMsg(1, e.toString());
            }
        }
        protected static Procedure delete = (DataBase db, String[] args) -> delete(db, args);

        /** Print resultSet returned from database **/
        private static void printReturnResultSet(ResultSet rs) throws SQLException {
            ArrayList<String[]> resultSetString = new ArrayList<>();
            ResultSetMetaData tableInfo = rs.getMetaData();
            int numCols = tableInfo.getColumnCount();

            //Set largest Column Widths array to all 0's
            int[] largestWidths = new int[numCols];
            for(int i = 0; i < largestWidths.length; i++){
                largestWidths[i] = 0;
            }

            //Get Column Names
            String[] colNames = new String[numCols];
            for(int i = 0; i < numCols; i++){
                colNames[i] = tableInfo.getColumnName(i + 1);

                //Get Data for largest column width
                if(colNames[i].length() > largestWidths[i]){
                    largestWidths[i] = colNames[i].length();
                }
            }
            resultSetString.add(colNames);

            //Get Row Info
            while(rs.next()){
                String[] row = new String[numCols];
                for(int i = 0; i < numCols; i++){
                    //The extra + "" is added to prevent the string from being null
                    row[i] = rs.getString(i + 1) + "";

                    //Get Data for largest column width
                    if(row[i].length() > largestWidths[i]){
                        largestWidths[i] = row[i].length();
                    }
                }
                resultSetString.add(row);
            }

            //Create format string
            String format = "";
            int lineLength = 0;
            for(int i = 0; i < largestWidths.length; i++){
                lineLength += largestWidths[i] + (i + 1 < largestWidths.length ? 3 : 0);
                format += "%-" + largestWidths[i] + "s" + (i + 1 < largestWidths.length ? " | " : "\r\n");
            }

            //Create line between headers and table instance
            String line = "";
            for(int i = 0; i < lineLength; i++){
                line += "-";
            }

            //Print result set to screen
            System.out.printf(format, (Object[]) resultSetString.get(0));
            System.out.println(line);
            for(int i = 1; i < resultSetString.size(); i++){
                System.out.printf(format, (Object[]) resultSetString.get(i));
            }
        }

        protected interface Procedure{
            void execute(DataBase db, String[] args);
        }
    }

    /////////////////////////////////
    // XXX Start of Database Class
    /////////////////////////////////

    public static class DataBase {

        private Connection connection;
        private Statement stm;

        public DataBase() throws ClassNotFoundException, SQLException {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + TARGET + "/test?verifyServerCertificate=false&useSSL=true&serverTimezone=UTC", USERNAME, PASSWORD);
            System.out.printf("Connection Established with %s\r\n\r\n", DB_NAME);
            connection.setAutoCommit(false);
        }

        /**
         * Provide a sql statement to get a result set
         * @return a result set for query sent
         * @throws SQLException
         */
        public ResultSet executeStatement(String s) throws SQLException {
            Pair<String, String[]> statement = addDBName(s);
            stm = connection.createStatement();
            ResultSet rs = stm.executeQuery(String.format(statement.getA(), (Object[]) statement.getB()));
            connection.commit();
            return rs;
        }

        /**
         * Provide a sql statement to get number of updated rows
         * @return number of updated rows
         * @throws SQLException
         */
        public int updateStatement(String s) throws SQLException{
            Pair<String, String[]> statement = addDBName(s);
            stm = connection.createStatement();
            int i = stm.executeUpdate(String.format(statement.getA(), (Object[]) statement.getB()));
            connection.commit();
            return i;
        }

        /**
         * Returns a pair where A is a formatted string and B is a List of DB name for every %s in A
         * @param s formatted string where %s will be replaced by the database name
         * @return A Pair where Pair.getA = formatted String and Pair.getB = List of DB name for every %s in formatted string
         */
        private Pair<String, String[]> addDBName(String s){
            //Find all the s flags in s format
            char[] charArray = s.toCharArray();
            int numSFlags = 0;
            boolean foundFlagStart = false;
            for(int i = 0; i < charArray.length; i++){
                if(foundFlagStart && (charArray[i] == 's' || charArray[i] == 'S')){
                    numSFlags++;
                }
                foundFlagStart = false;

                if(charArray[i] == '%'){
                    foundFlagStart = true;
                }
            }

            //Create an array of DB name for every s flag found for format string
            String[] dbNameArray = new String[numSFlags];
            for(int i = 0; i < dbNameArray.length; i++){
                dbNameArray[i] = DB_NAME;
            }
            return new Pair<>(s, dbNameArray);
        }

        public void closeDataBase(){
            closeStatement();
            closeConnection();
            if(error != ArgError.CloseDataBase){
                System.out.println("\r\nDatabase Connection Closed.");
            }
        }

        private void closeStatement(){
            try {
                if(stm != null) {
                    stm.close();
                }
            }catch (SQLException e){
                error = ArgError.CloseDataBase;
                error.addAttachedMsg(1, "Unable to close the Database Statement.");
            }
        }

        private void closeConnection(){
            try {
                if(error == ArgError.DataBaseError){
                    connection.rollback();
                }
                if(connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            }catch (SQLException e){
                error = ArgError.CloseDataBase;
                error.addAttachedMsg(1, "Unable to close the Database Connection.");
            }
        }
    }

    ////////////////////////////
    // XXX Start of Pair Class
    ////////////////////////////

    public static class Pair<A, B> {

        private A a;
        private B b;

        public Pair(A a, B b){
            this.a = a;
            this.b = b;
        }

        public A getA() {
            return a;
        }

        public void setA(A a) {
            this.a = a;
        }

        public B getB() {
            return b;
        }

        public void setB(B b) {
            this.b = b;
        }
    }
}
