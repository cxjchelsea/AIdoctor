package com.aidoctor.diagnosis.runtime.u07;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/** src/test ONLY: intercepts fixed URL without a production factory/callback API. */
final class U07D5PTestDriver implements Driver, AutoCloseable {
    enum Fault { NONE, CONNECT, CONFIG, BEGIN, BODY, COMMIT_BEFORE, COMMIT_AFTER,
        BODY_ROLLBACK, BODY_ROLLBACK_CLOSE, REJECT_ROLLBACK, CLOSE, READ,
        READ_CLOSE, READ_ROLLBACK, READ_ROLLBACK_CLOSE, READ_FAIL_CLOSE, FATAL }
    static final class Trace {
        Fault fault = Fault.NONE;
        int connections, queries, statements, commits, rollbacks, closes;
        SQLException primary, rollbackError, closeError;
        final AssertionError fatal = new AssertionError("injected fatal Error");
    }
    final ThreadLocal<Trace> traces = ThreadLocal.withInitial(Trace::new);
    private final Driver delegate;
    private final List<Driver> saved = new ArrayList<>();
    U07D5PTestDriver() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Enumeration<Driver> all = DriverManager.getDrivers();
        while (all.hasMoreElements()) {
            Driver d = all.nextElement();
            if (d.getClass().getName().equals("com.mysql.cj.jdbc.Driver")) {
                saved.add(d); DriverManager.deregisterDriver(d);
            }
        }
        delegate = (Driver) Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        DriverManager.registerDriver(this);
    }
    Trace mode(Fault fault) {
        Trace t = new Trace(); t.fault = fault;
        t.primary = new SQLException("injected " + fault, "08006", 0);
        t.rollbackError = new SQLException("injected rollback", "08006", 0);
        t.closeError = new SQLException("injected close", "08006", 0);
        traces.set(t); return t;
    }
    private static Object invoke(Method m, Object target, Object[] args) throws Throwable {
        try { return m.invoke(target, args); }
        catch (InvocationTargetException error) { throw error.getCause(); }
    }
    public Connection connect(String url, Properties properties) throws SQLException {
        if (!acceptsURL(url)) return null;
        Trace t = traces.get(); t.connections++;
        if (t.fault == Fault.CONNECT) throw t.primary;
        Connection c = delegate.connect(url, properties);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, m, args) -> {
                    String name = m.getName();
                    if (name.equals("setTransactionIsolation") && t.fault == Fault.CONFIG) throw t.primary;
                    if (name.equals("setAutoCommit") && t.fault == Fault.BEGIN) throw t.primary;
                    if (name.equals("commit")) {
                        t.commits++;
                        if (t.fault == Fault.COMMIT_BEFORE) throw t.primary;
                        Object value = invoke(m, c, args);
                        if (t.fault == Fault.COMMIT_AFTER) throw t.primary;
                        return value;
                    }
                    if (name.equals("rollback")) {
                        t.rollbacks++;
                        if (t.fault == Fault.BODY_ROLLBACK || t.fault == Fault.BODY_ROLLBACK_CLOSE
                                || t.fault == Fault.REJECT_ROLLBACK || t.fault == Fault.READ_ROLLBACK
                                || t.fault == Fault.READ_ROLLBACK_CLOSE) throw t.rollbackError;
                    }
                    if (name.equals("close")) {
                        t.closes++;
                        Object value = invoke(m, c, args);
                        if (t.fault == Fault.CLOSE || t.fault == Fault.BODY_ROLLBACK_CLOSE
                                || t.fault == Fault.READ_CLOSE || t.fault == Fault.READ_ROLLBACK_CLOSE
                                || t.fault == Fault.READ_FAIL_CLOSE) throw t.closeError;
                        return value;
                    }
                    if (name.equals("prepareStatement")) {
                        t.statements++;
                        String sql = (String) args[0];
                        PreparedStatement p = (PreparedStatement) invoke(m, c, args);
                        return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                                new Class<?>[]{PreparedStatement.class}, (sp, sm, sa) -> {
                                    if (sm.getName().equals("executeQuery")) {
                                        t.queries++;
                                        if (sql.contains("FROM (SELECT CAST") && (t.fault == Fault.READ
                                                || t.fault == Fault.READ_FAIL_CLOSE)) throw t.primary;
                                    }
                                    if (sm.getName().equals("executeUpdate") && sql.startsWith("INSERT INTO u07_event_application")) {
                                        if (t.fault == Fault.FATAL) throw t.fatal;
                                        if (t.fault == Fault.BODY || t.fault == Fault.BODY_ROLLBACK
                                                || t.fault == Fault.BODY_ROLLBACK_CLOSE) throw t.primary;
                                    }
                                    return invoke(sm, p, sa);
                                });
                    }
                    return invoke(m, c, args);
                });
    }
    public boolean acceptsURL(String url) {
        return U07SyntheticTestTarget.D3.jdbcUrl().equals(url) || U07SyntheticTestTarget.D4.jdbcUrl().equals(url);
    }
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
    public int getMajorVersion() { return 1; }
    public int getMinorVersion() { return 0; }
    public boolean jdbcCompliant() { return false; }
    public Logger getParentLogger() { return Logger.getLogger("u07.synthetic.test"); }
    public void close() throws SQLException {
        DriverManager.deregisterDriver(this);
        for (Driver d : saved) DriverManager.registerDriver(d);
        traces.remove();
    }
}
