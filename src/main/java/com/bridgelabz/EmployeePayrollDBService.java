package com.bridgelabz;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeePayrollDBService {

    private static EmployeePayrollDBService employeePayrollDBService;
    private PreparedStatement employeePayrollDataStatement;
    private EmployeePayrollDBService(){
    }

    public static EmployeePayrollDBService getInstance(){
        if (employeePayrollDBService == null){
            employeePayrollDBService = new EmployeePayrollDBService();
        }
        return employeePayrollDBService;
    }

    private Connection getConnection() throws SQLException {
        String jdbcURL = "jdbc:mysql://localhost:3306/employee_payroll_service?useSSL=false";
        String userName = "root";
        String password = "mySQL051276R@609";
        Connection connection;
        System.out.println("Connecting to database : " +jdbcURL);
        connection = DriverManager.getConnection(jdbcURL,userName,password);
        System.out.println("Connection is successful!!!" + connection);
        return connection;
    }

    public List<EmployeePayrollData> readData() {
        String sql = "SELECT * FROM employee_payroll; ";
        return this.getEmployeePayrollDataUsingDB(sql);
    }

    public List<EmployeePayrollData> getEmployeeForDateRange(LocalDate startDate, LocalDate endDate) {
       String sql = String.format("SELECT *FROM employee_payroll WHERE START BETWEEN '%s' AND '%s';" ,
                                  Date.valueOf(startDate), Date.valueOf(endDate));
       return this.getEmployeePayrollDataUsingDB(sql);
    }

    public Map<String, Double> getAverageSalaryByGender() {
        String sql = "SELECT gender, AVG(salary) as avg_salary FROM employee_payroll GROUP BY gender;";
        Map<String, Double> genderToAverageSalaryMap = new HashMap<>();
        try(Connection connection = this.getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while(resultSet.next()){
                String gender = resultSet.getString("gender");
                double salary = resultSet.getDouble("avg_salary");
                genderToAverageSalaryMap.put(gender, salary);
            }
        } catch (SQLException e ){
            e.printStackTrace();
        }
        return genderToAverageSalaryMap;
    }

    private List<EmployeePayrollData> getEmployeePayrollDataUsingDB(String sql) {
        List<EmployeePayrollData> employeePayrollList = new ArrayList<>();
        try (Connection connection = this.getConnection()){
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            employeePayrollList = this.getEmployeePayrollData(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return employeePayrollList;
    }

    public List<EmployeePayrollData> getEmployeePayrollData(String name) {
        List<EmployeePayrollData> employeePayrollList = null;
        if(this.employeePayrollDataStatement == null)
            this.prepareStatementForEmployeeData();
        try{
            employeePayrollDataStatement.setString(1, name);
            ResultSet resultSet = employeePayrollDataStatement.executeQuery();
            employeePayrollList = this.getEmployeePayrollData(resultSet);
        } catch (SQLException e ){
            e.printStackTrace();
        }
        return employeePayrollList;
    }

    private List<EmployeePayrollData> getEmployeePayrollData(ResultSet resultSet) {
        List<EmployeePayrollData> employeePayrollList = new ArrayList<>();
        try{
            while(resultSet.next()){
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                double salary = resultSet.getDouble("salary");
                LocalDate startDate = resultSet.getDate("start").toLocalDate();
                employeePayrollList.add(new EmployeePayrollData(id, name, salary, startDate));
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return employeePayrollList;
    }

    private void prepareStatementForEmployeeData() {
        try{
            Connection connection = this.getConnection();
            String sql = "SELECT * FROM employee_payroll WHERE name = ?";
            employeePayrollDataStatement = connection.prepareStatement(sql);
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public int updateEmployeeData(String name, double salary) {
        return this.updateEmployeeDataUsingStatement(name, salary);
    }

    private int updateEmployeeDataUsingStatement(String name, double salary) {
        String sql = String.format("update employee_payroll set salary = %.2f where name = '%s';", salary, name);
        try(Connection connection = this.getConnection()) {
            Statement statement = connection.createStatement();
            return statement.executeUpdate(sql);
        } catch (SQLException e ){
            e.printStackTrace();
        }
        return 0;
    }

    public EmployeePayrollData addEmployeeToPayrollUC7(String name, double salary, LocalDate startDate, String gender) {
        int employeeId = -1;
        EmployeePayrollData employeePayrollData = null;
        String sql = String.format("INSERT INTO employee_payroll (name, salary, start, gender) " +
                                   "VALUES ( '%s', '%s', '%s', '%s' )", name, salary, Date.valueOf(startDate), gender);
        try(Connection connection = this.getConnection()) {
            Statement statement = connection.createStatement();
            int rowAffected = statement.executeUpdate(sql, statement.RETURN_GENERATED_KEYS);
            if(rowAffected == 1){
                ResultSet resultSet = statement.getGeneratedKeys();
                if(resultSet.next()) employeeId = resultSet.getInt(1);
            }
            employeePayrollData =new EmployeePayrollData(employeeId, name, salary, startDate);
        } catch (SQLException e){
            e.printStackTrace();
        }
        return employeePayrollData;
    }

    public EmployeePayrollData addEmployeeToPayrollUC8(String name, double salary, LocalDate startDate, String gender) {
        int employeeId = -1;
        Connection connection = null;
        EmployeePayrollData employeePayrollData = null;
        try{
            connection = this.getConnection();
            connection.setAutoCommit(false);
        } catch(SQLException e){
            e.printStackTrace();
        }
        try(Statement statement = connection.createStatement()) {
            String sql = String.format("INSERT INTO employee_payroll (name, salary, start, gender) " +
                                       "VALUES ( '%s', '%s', '%s', '%s' )", name, salary, Date.valueOf(startDate), gender);
            int rowAffected = statement.executeUpdate(sql, statement.RETURN_GENERATED_KEYS);
            if(rowAffected == 1){
                ResultSet resultSet = statement.getGeneratedKeys();
                if(resultSet.next()) employeeId = resultSet.getInt(1);
            }
        } catch (SQLException e){
            e.printStackTrace();
            try {
                connection.rollback();
                return employeePayrollData;
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        try(Statement statement = connection.createStatement()) {
            double deductions = salary * 0.2;
            double taxablePay = salary - deductions;
            double tax = taxablePay * 0.1;
            double netPay = salary - tax;
            String sql = String.format("INSERT INTO payroll_details" +
                    "( employee_id, basic_pay, deductions, taxable_pay, tax, net_pay) VALUES" +
                    "( %s, %s, %s, %s, %s, %s )", employeeId, salary, deductions, taxablePay, tax, netPay);
            int rowAffected = statement.executeUpdate(sql);
            if(rowAffected == 1){
                employeePayrollData = new EmployeePayrollData(employeeId, name, salary, startDate);
            }
        }catch(SQLException e){
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        try {
            connection.rollback();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }finally {
            if(connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return employeePayrollData;
    }
}
