/* SpecializedHospitalManagementClean.java
   Single-file Java Swing frontend (clean, non-animated).
   - Uses online static background images
   - Replaces "Sex" with "Gender" everywhere
   - Uses specific exception handling to avoid broad hints
   - Starts from Login page
   - DB credentials already set to the username/password you provided
   - Compile/run with MySQL Connector/J on classpath
*/

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SpecializedHospitalManagementClean extends JFrame {

    // ---- DB credentials (you gave these)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/shm_db?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Prasanna@2886";

    // ---- static background image URLs (static theme)
    private static final String BG_LOGIN_URL = "https://images.unsplash.com/photo-1586773860418-6ec7c9f9a4d6?w=1600&q=80";
    private static final String BG_FORM_URL  = "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=1600&q=80";
    private static final String BG_HOSP_URL  = "https://images.unsplash.com/photo-1582719478250-2f8d86f7f0f4?w=1600&q=80";
    private static final String LOGO_URL     = "https://images.unsplash.com/photo-1580281657520-0b1eae3a3f4?w=800&q=80";

    // UI
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    // Images cached
    private Image bgLoginImg, bgFormImg, bgHospImg, logoImg;

    // Runtime state
    private int loggedUserId = -1;
    private int lastPatientId = -1;
    private Integer bookedAmbulanceId = null;
    private Integer lastBookingId = null;

    // DB helper
    private final DBHelper db;

    public SpecializedHospitalManagementClean() {
        super("Specialized Hospital Management");
        db = new DBHelper(DB_URL, DB_USER, DB_PASS);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // load images (attempt, with fallback)
        bgLoginImg = loadImage(BG_LOGIN_URL, 1600, 900);
        bgFormImg  = loadImage(BG_FORM_URL, 1600, 900);
        bgHospImg  = loadImage(BG_HOSP_URL, 1600, 900);
        logoImg    = loadImage(LOGO_URL, 400, 200);

        // panels
        root.add(new LoginPanel(), "login");
        root.add(new SignupPanel(), "signup");
        root.add(new PatientPanel(), "patient");
        root.add(new AmbulancePanel(), "ambulance");
        root.add(new HospitalPanel(), "hospital");
        root.add(new DoctorPanel(), "doctor");
        root.add(new WardFoodPanel(), "wardfood");
        root.add(new BillingPanel(), "billing");
        root.add(new SummaryPanel(), "summary");

        add(root);
        cardLayout.show(root, "login"); // always start at login
        setVisible(true);
    }

    // ---------- image loader with safe fallback ----------
    private Image loadImage(String urlStr, int w, int h) {
        try {
            // prefer URI.create to avoid deprecated constructor warning
            URL u = URI.create(urlStr).toURL();
            BufferedImage img = ImageIO.read(u);
            if (img == null) throw new IOException("image read returned null");
            return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        } catch (IOException | IllegalArgumentException e) {
            // fallback: gradient image
            BufferedImage fallback = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = fallback.createGraphics();
            g.setPaint(new GradientPaint(0, 0, new Color(230, 240, 255), w, h, new Color(210, 230, 250)));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return fallback;
        }
    }

    // ---------- simple DB helper (specific exceptions) ----------
    private static class DBHelper {
        private final String url, user, pass;
        DBHelper(String url, String user, String pass) {
            this.url = url; this.user = user; this.pass = pass;
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                // driver not found; the app will show DB errors when trying to connect
            }
        }

        Connection connect() throws SQLException {
            return DriverManager.getConnection(url, user, pass);
        }

        boolean createUser(String username, String email, String password) throws SQLException {
            String check = "SELECT id FROM users WHERE username=? OR email=?";
            String insert = "INSERT INTO users(username,email,password) VALUES(?,?,?)";
            try (Connection c = connect();
                 PreparedStatement psCheck = c.prepareStatement(check)) {
                psCheck.setString(1, username);
                psCheck.setString(2, email);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) return false;
                }
                try (PreparedStatement psIns = c.prepareStatement(insert)) {
                    psIns.setString(1, username);
                    psIns.setString(2, email);
                    psIns.setString(3, password);
                    psIns.executeUpdate();
                    return true;
                }
            }
        }

        int authenticate(String username, String password) throws SQLException {
            String q = "SELECT id FROM users WHERE username=? AND password=?";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(q)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                    return -1;
                }
            }
        }

        int insertPatient(String name, int age, String address, String mobile,
                          String guardianName, String guardianRelation, String guardianMobile,
                          String disease, boolean emergency, boolean ambulanceRequired) throws SQLException {
            String sql = "INSERT INTO patients(name, age, address, mobile, guardian_name, guardian_relation, guardian_mobile, disease, emergency, ambulance_required) VALUES(?,?,?,?,?,?,?,?,?,?)";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setInt(2, age);
                ps.setString(3, address);
                ps.setString(4, mobile);
                ps.setString(5, guardianName);
                ps.setString(6, guardianRelation);
                ps.setString(7, guardianMobile);
                ps.setString(8, disease);
                ps.setBoolean(9, emergency);
                ps.setBoolean(10, ambulanceRequired);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                    return -1;
                }
            }
        }

        // ensure sample hospitals if empty
        void ensureHospitals() throws SQLException {
            try (Connection c = connect();
                 PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM hospitals");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String ins = "INSERT INTO hospitals (name, location, specialization, terms, rating) VALUES (?,?,?,?,?)";
                    try (PreparedStatement ps2 = c.prepareStatement(ins)) {
                        for (int i = 1; i <= 10; i++) {
                            ps2.setString(1, "Specialized Hospital " + i);
                            ps2.setString(2, "City " + i);
                            ps2.setString(3, "Specialty " + ((i % 5) + 1));
                            ps2.setString(4, "Hospital terms apply.");
                            ps2.setDouble(5, 4.0 + (i % 5) * 0.1);
                            ps2.executeUpdate();
                        }
                    }
                }
            }
        }

        List<Hospital> listHospitals() throws SQLException {
            List<Hospital> out = new ArrayList<>();
            String q = "SELECT id, name, location, specialization FROM hospitals";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(q); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Hospital h = new Hospital();
                    h.id = rs.getInt(1);
                    h.name = rs.getString(2);
                    h.location = rs.getString(3);
                    h.specialization = rs.getString(4);
                    out.add(h);
                }
            }
            return out;
        }

        Integer ensureAndGetAmbulance() throws SQLException {
            try (Connection c = connect();
                 PreparedStatement psCount = c.prepareStatement("SELECT COUNT(*) FROM ambulance");
                 ResultSet rs = psCount.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String ins = "INSERT INTO ambulance(driver_name, driver_age, driver_gender, driver_mobile, ambulance_number, nurse_name, nurse_age, nurse_gender, nurse_mobile, booked) VALUES(?,?,?,?,?,?,?,?,?,FALSE)";
                    try (PreparedStatement ps = c.prepareStatement(ins)) {
                        ps.setString(1, "Ravi Kumar");
                        ps.setInt(2, 36);
                        ps.setString(3, "Male");
                        ps.setString(4, "9845012345");
                        ps.setString(5, "AMB-1001");
                        ps.setString(6, "Priya");
                        ps.setInt(7, 29);
                        ps.setString(8, "Female");
                        ps.setString(9, "9845099999");
                        ps.executeUpdate();
                    }
                }
            }
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("SELECT id FROM ambulance LIMIT 1"); ResultSet rs2 = ps.executeQuery()) {
                if (rs2.next()) return rs2.getInt(1);
                return null;
            }
        }

        Ambulance getAmbulance(int aid) throws SQLException {
            String q = "SELECT id, driver_name, driver_age, driver_gender, driver_mobile, ambulance_number, nurse_name, nurse_age, nurse_gender, nurse_mobile FROM ambulance WHERE id=?";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(q)) {
                ps.setInt(1, aid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Ambulance a = new Ambulance();
                        a.id = rs.getInt(1);
                        a.driverName = rs.getString(2);
                        a.driverAge = rs.getInt(3);
                        a.driverGender = rs.getString(4);
                        a.driverMobile = rs.getString(5);
                        a.ambulanceNumber = rs.getString(6);
                        a.nurseName = rs.getString(7);
                        a.nurseAge = rs.getInt(8);
                        a.nurseGender = rs.getString(9);
                        a.nurseMobile = rs.getString(10);
                        return a;
                    }
                    return null;
                }
            }
        }

        int createBooking(int userId, int patientId, int hospitalId, Integer ambulanceId, String roomType, String foodPlan, double total) throws SQLException {
            String ins = "INSERT INTO bookings(user_id, patient_id, hospital_id, doctor_id, ambulance_id, booking_date) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                if (userId > 0) ps.setInt(1, userId); else ps.setNull(1, Types.INTEGER);
                ps.setInt(2, patientId);
                if (hospitalId > 0) ps.setInt(3, hospitalId); else ps.setNull(3, Types.INTEGER);
                ps.setNull(4, Types.INTEGER);
                if (ambulanceId != null) ps.setInt(5, ambulanceId); else ps.setNull(5, Types.INTEGER);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                    return -1;
                }
            }
        }

        void saveReview(int hospitalId, int rating, String text) throws SQLException {
            String ins = "INSERT INTO reviews(hospital_id, user_id, rating, text, created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP)";
            try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setInt(1, hospitalId);
                ps.setNull(2, Types.INTEGER);
                ps.setInt(3, rating);
                ps.setString(4, text);
                ps.executeUpdate();
            }
        }
    } // end DBHelper

    // ---------- Models ----------
    private static class Ambulance {
        int id; String driverName; int driverAge; String driverGender; String driverMobile; String ambulanceNumber;
        String nurseName; int nurseAge; String nurseGender; String nurseMobile;
    }
    private static class Hospital {
        int id; String name; String location; String specialization;
        public String toString() { return name + " (" + specialization + ") - " + location; }
    }

    // ---------------- UI Panels ----------------

    private abstract class BackgroundPanel extends JPanel {
        final Image bg;
        BackgroundPanel(Image bgImage) { this.bg = bgImage; setLayout(null); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bg != null) g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // ---- LOGIN
    private class LoginPanel extends BackgroundPanel {
        JTextField tfUser;
        JPasswordField pf;
        LoginPanel() {
            super(bgLoginImg);
            JLabel logo = new JLabel(new ImageIcon(logoImg));
            logo.setBounds(30, 15, 360, 90); add(logo);

            JLabel title = new JLabel("Specialized Hospital Scheme", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 30)); title.setForeground(Color.WHITE);
            title.setBounds(0, 30, 1100, 120); add(title);

            JPanel box = new JPanel(null); box.setBackground(new Color(255,255,255,220)); box.setBounds(320, 190, 460, 260);
            add(box);

            JLabel l1 = new JLabel("Username:"); l1.setBounds(40, 30, 90, 24); box.add(l1);
            tfUser = new JTextField(); tfUser.setBounds(140,30,270,26); box.add(tfUser);

            JLabel l2 = new JLabel("Password:"); l2.setBounds(40, 80, 90, 24); box.add(l2);
            pf = new JPasswordField(); pf.setBounds(140,80,270,26); box.add(pf);

            JButton btnLogin = new JButton("Login"); btnLogin.setBounds(140, 130, 120, 34); box.add(btnLogin);
            JButton btnSignup = new JButton("Sign Up"); btnSignup.setBounds(290, 130, 120, 34); box.add(btnSignup);

            btnLogin.addActionListener(e -> {
                String u = tfUser.getText().trim();
                String p = new String(pf.getPassword());
                if (u.isEmpty() || p.isEmpty()) {
                    showDialog("Enter username and password", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    int uid = db.authenticate(u, p);
                    if (uid > 0) {
                        loggedUserId = uid;
                        showDialog("Login successful", "Success", JOptionPane.INFORMATION_MESSAGE);
                        cardLayout.show(root, "patient");
                    } else {
                        showDialog("Invalid username or password", "Login Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (SQLException ex) {
                    showDialog("Database error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnSignup.addActionListener(e -> cardLayout.show(root, "signup"));
        }
    }

    // ---- SIGNUP
    private class SignupPanel extends BackgroundPanel {
        JTextField tfUser, tfEmail;
        JPasswordField pf;
        SignupPanel() {
            super(bgFormImg);
            JLabel logo = new JLabel(new ImageIcon(logoImg));
            logo.setBounds(30, 15, 360, 90); add(logo);

            JLabel title = new JLabel("Create an Account", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 28)); title.setForeground(Color.WHITE); title.setBounds(0, 30, 1100, 120); add(title);

            JPanel box = new JPanel(null); box.setBackground(new Color(255,255,255,230)); box.setBounds(300, 180, 500, 300);
            add(box);

            JLabel l1 = new JLabel("Username:"); l1.setBounds(40, 30, 100, 24); box.add(l1);
            tfUser = new JTextField(); tfUser.setBounds(160,30,280,26); box.add(tfUser);

            JLabel l2 = new JLabel("Email:"); l2.setBounds(40, 80, 100, 24); box.add(l2);
            tfEmail = new JTextField(); tfEmail.setBounds(160,80,280,26); box.add(tfEmail);

            JLabel l3 = new JLabel("Password:"); l3.setBounds(40, 130, 100, 24); box.add(l3);
            pf = new JPasswordField(); pf.setBounds(160,130,280,26); box.add(pf);

            JButton btnCreate = new JButton("Create Account"); btnCreate.setBounds(160, 190, 150, 34); box.add(btnCreate);
            JButton btnBack = new JButton("Back to Login"); btnBack.setBounds(320, 190, 120, 34); box.add(btnBack);

            btnCreate.addActionListener(e -> {
                String u = tfUser.getText().trim();
                String em = tfEmail.getText().trim();
                String p = new String(pf.getPassword());
                if (u.isEmpty() || em.isEmpty() || p.isEmpty()) {
                    showDialog("Fill all fields", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    boolean ok = db.createUser(u, em, p);
                    if (ok) {
                        showDialog("Account created successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                        cardLayout.show(root, "login");
                    } else {
                        showDialog("Username or email already exists", "Already exists", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (SQLException ex) {
                    showDialog("Database error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnBack.addActionListener(e -> cardLayout.show(root, "login"));
        }
    }

    // ---- PATIENT
    private class PatientPanel extends BackgroundPanel {
        JTextField tfName, tfAge, tfAddress, tfMobile, tfGName, tfGRel, tfGMobile, tfDisease;
        JCheckBox cbEmergency, cbAmb;
        PatientPanel() {
            super(bgFormImg);
            JLabel title = new JLabel("Patient Details", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 28));
            title.setForeground(Color.WHITE); title.setBounds(0, 20, 1100, 60); add(title);

            int startY = 110;
            tfName = addField("Name:", 180, startY); startY += 40;
            tfAge  = addField("Age:", 180, startY); startY += 40;
            // Gender input replaced — dropdown
            addLabel("Gender:", 180, startY);
            JComboBox<String> cbGender = new JComboBox<>(new String[]{"Male","Female","Other"});
            cbGender.setBounds(340, startY, 220, 26); add(cbGender);
            startY += 40;

            tfAddress = addField("Address:", 180, startY); startY += 40;
            tfMobile  = addField("Mobile:", 180, startY); startY += 40;
            tfGName   = addField("Guardian Name:", 180, startY); startY += 40;
            tfGRel    = addField("Guardian Relation:", 180, startY); startY += 40;
            tfGMobile = addField("Guardian Mobile:", 180, startY); startY += 40;
            tfDisease = addField("Disease / Symptoms:", 180, startY); startY += 40;

            cbEmergency = new JCheckBox("Emergency"); cbEmergency.setBounds(180, startY, 140, 24); add(cbEmergency);
            cbAmb = new JCheckBox("Require Ambulance"); cbAmb.setBounds(340, startY, 180, 24); add(cbAmb);

            JButton btnSave = new JButton("Verify & Continue"); btnSave.setBounds(740, startY+40, 200, 36); add(btnSave);
            JButton btnLogout = new JButton("Logout"); btnLogout.setBounds(600, startY+40, 120, 36); add(btnLogout);

            btnSave.addActionListener(e -> {
                String name = tfName.getText().trim();
                int age = 0;
                try {
                    age = Integer.parseInt(tfAge.getText().trim().isEmpty() ? "0" : tfAge.getText().trim());
                } catch (NumberFormatException ex) {
                    showDialog("Invalid age", "Error", JOptionPane.ERROR_MESSAGE); return;
                }
                String address = tfAddress.getText().trim();
                String mobile = tfMobile.getText().trim();
                String gname = tfGName.getText().trim();
                String grel = tfGRel.getText().trim();
                String gmobile = tfGMobile.getText().trim();
                String disease = tfDisease.getText().trim();
                boolean emergency = cbEmergency.isSelected();
                boolean ambulanceNeeded = cbAmb.isSelected();

                if (name.isEmpty()) { showDialog("Enter patient name", "Error", JOptionPane.ERROR_MESSAGE); return; }

                try {
                    int pid = db.insertPatient(name, age, address, mobile, gname, grel, gmobile, disease, emergency, ambulanceNeeded);
                    if (pid <= 0) { showDialog("Could not save patient", "DB Error", JOptionPane.ERROR_MESSAGE); return; }
                    lastPatientId = pid;
                    showDialog("Patient's detail verified successfully", "Verified", JOptionPane.INFORMATION_MESSAGE);

                    if (emergency && ambulanceNeeded) {
                        int r = JOptionPane.showConfirmDialog(this, "Emergency detected. Request ambulance now?", "Emergency", JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION) {
                            Integer aid = db.ensureAndGetAmbulance();
                            if (aid != null) {
                                bookedAmbulanceId = aid;
                                Ambulance a = db.getAmbulance(aid);
                                showDialog("Ambulance booked successfully\nDriver: " + a.driverName + " Mobile: " + a.driverMobile, "Ambulance", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                showDialog("No ambulance available", "Info", JOptionPane.WARNING_MESSAGE);
                            }
                        } else {
                            showDialog("Emergency recorded (no ambulance requested)", "Notice", JOptionPane.WARNING_MESSAGE);
                        }
                    } else if (emergency) {
                        showDialog("Emergency recorded", "Notice", JOptionPane.WARNING_MESSAGE);
                    }

                    // ensure hospitals and go forward
                    db.ensureHospitals();
                    cardLayout.show(root, "hospital");

                } catch (SQLException ex) {
                    showDialog("Database error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnLogout.addActionListener(e -> {
                int r = JOptionPane.showConfirmDialog(this, "Logout and return to Login?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) {
                    loggedUserId = -1;
                    lastPatientId = -1;
                    bookedAmbulanceId = null;
                    lastBookingId = null;
                    cardLayout.show(root, "login");
                }
            });
        }

        private JTextField addField(String labelText, int x, int y) {
            addLabel(labelText, x, y);
            JTextField tf = new JTextField(); tf.setBounds(x + 160, y, 520, 26); add(tf); return tf;
        }

        private void addLabel(String labelText, int x, int y) {
            JLabel lbl = new JLabel(labelText); lbl.setBounds(x, y, 150, 24); add(lbl);
        }
    }

    // ---- Ambulance panel (short) ----
    private class AmbulancePanel extends BackgroundPanel {
        AmbulancePanel() {
            super(bgFormImg);
            JLabel title = new JLabel("Ambulance", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 26)); title.setForeground(Color.WHITE); title.setBounds(0, 30, 1100, 40); add(title);
            JLabel info = new JLabel("<html><center>Ambulance request processed (simulated).</center></html>", SwingConstants.CENTER); info.setBounds(250, 200, 600, 80); add(info);
            JButton next = new JButton("Continue"); next.setBounds(470, 320, 160, 36); add(next);
            next.addActionListener(e -> cardLayout.show(root, "hospital"));
        }
    }

    // ---- Hospital selection ----
    private class HospitalPanel extends BackgroundPanel {
        JComboBox<Hospital> combo;
        HospitalPanel() {
            super(bgHospImg);
            JLabel title = new JLabel("Choose Hospital", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 26)); title.setForeground(Color.WHITE); title.setBounds(0, 30, 1100, 40); add(title);

            combo = new JComboBox<>(); combo.setBounds(200, 150, 700, 30); add(combo);
            JButton refresh = new JButton("Refresh"); refresh.setBounds(200, 200, 120, 30); add(refresh);
            JButton choose = new JButton("Select & Continue"); choose.setBounds(340, 200, 160, 30); add(choose);
            JButton skip = new JButton("Skip & Continue"); skip.setBounds(520,200,160,30); add(skip);

            refresh.addActionListener(e -> loadHospitals());
            choose.addActionListener(e -> {
                Hospital h = (Hospital) combo.getSelectedItem();
                if (h == null) { showDialog("Select a hospital", "Info", JOptionPane.WARNING_MESSAGE); return; }
                showDialog("Hospital selected: " + h.name, "Selected", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(root, "doctor");
            });
            skip.addActionListener(e -> cardLayout.show(root, "doctor"));

            loadHospitals();
        }

        private void loadHospitals() {
            combo.removeAllItems();
            try {
                List<Hospital> list = db.listHospitals();
                for (Hospital h : list) combo.addItem(h);
            } catch (SQLException ex) {
                showDialog("Database error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ---- Doctor panel ----
    private class DoctorPanel extends BackgroundPanel {
        DoctorPanel() {
            super(bgHospImg);
            JLabel title = new JLabel("Doctor Details", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 26)); title.setForeground(Color.WHITE); title.setBounds(0,30,1100,40); add(title);
            JTextArea ta = new JTextArea(); ta.setBounds(200,120,700,360); ta.setOpaque(false); ta.setForeground(Color.WHITE);
            ta.setText("Primary Doctor: Dr. A. Kumar\nAge: 45\nField: Cardiology\nExperience: 18 years\n\nSecondary Doctor: Dr. M. Sharma\nAge: 39\nField: General Medicine\nExperience: 12 years\n\n(Placeholders for photos / videos / reviews)");
            add(ta);
            JButton done = new JButton("Done & Continue"); done.setBounds(470,500,160,36); add(done);
            done.addActionListener(e -> { showDialog("Doctor verified", "Verified", JOptionPane.INFORMATION_MESSAGE); cardLayout.show(root, "wardfood"); });
        }
    }

    // ---- Ward & Food ----
    private class WardFoodPanel extends BackgroundPanel {
        JComboBox<String> cbRoom, cbFood;
        WardFoodPanel() {
            super(bgFormImg);
            JLabel title = new JLabel("Ward & Food Preferences", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 26)); title.setForeground(Color.WHITE); title.setBounds(0, 20, 1100, 40); add(title);

            JLabel l1 = new JLabel("Room Type:"); l1.setBounds(220, 140, 120, 24); add(l1);
            cbRoom = new JComboBox<>(new String[] {"AC Single - 5000","Non-AC Single - 3000","2-Sharing - 3000","4-Sharing - 1500"}); cbRoom.setBounds(360,140,420,26); add(cbRoom);

            JLabel l2 = new JLabel("Food Plan:"); l2.setBounds(220, 190, 120, 24); add(l2);
            cbFood = new JComboBox<>(new String[] {"Standard - 300","Protein - 500","Vegetarian - 250","Special - 800"}); cbFood.setBounds(360,190,300,26); add(cbFood);

            JButton next = new JButton("Continue to Billing"); next.setBounds(460, 260, 200, 36); add(next);
            next.addActionListener(e -> cardLayout.show(root, "billing"));
        }
    }

    // ---- Billing ----
    private class BillingPanel extends BackgroundPanel {
        JTextField tfDoctorFee, tfMisc;
        JComboBox<String> cbRoom, cbFood;
        JCheckBox cbAmbUsed;
        JLabel lblTotal;
        BillingPanel() {
            super(bgFormImg);
            JLabel title = new JLabel("Billing & Payment", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 26)); title.setForeground(Color.WHITE); title.setBounds(0, 20, 1100, 40); add(title);

            tfDoctorFee = new JTextField("1500"); tfDoctorFee.setBounds(360,110,120,26); add(tfDoctorFee);
            add(new JLabel("Doctor Fee (₹):")).setBounds(240,110,120,24);

            cbRoom = new JComboBox<>(new String[] {"AC Single - 5000","Non-AC Single - 3000","2-Sharing - 3000","4-Sharing - 1500"}); cbRoom.setBounds(360,150,300,26); add(cbRoom);
            add(new JLabel("Room Type:")).setBounds(240,150,120,24);

            cbFood = new JComboBox<>(new String[] {"Standard - 300","Protein - 500","Vegetarian - 250","Special - 800"}); cbFood.setBounds(360,190,240,26); add(cbFood);
            add(new JLabel("Food Plan:")).setBounds(240,190,120,24);

            cbAmbUsed = new JCheckBox("Ambulance used (₹1500)"); cbAmbUsed.setBounds(360,230,220,24); add(cbAmbUsed);
            tfMisc = new JTextField("0"); tfMisc.setBounds(360,270,120,26); add(tfMisc); add(new JLabel("Misc (₹):")).setBounds(240,270,120,24);

            JButton btnCalc = new JButton("Calculate Total"); btnCalc.setBounds(360, 320, 160, 34); add(btnCalc);
            lblTotal = new JLabel("Total: ₹0.00"); lblTotal.setFont(new Font("SansSerif", Font.BOLD, 18)); lblTotal.setBounds(360, 370, 300, 30); add(lblTotal);

            JButton btnPay = new JButton("Pay Online"); btnPay.setBounds(360,420,140,36); add(btnPay);
            JButton btnBack = new JButton("Back to Ward/Food"); btnBack.setBounds(520,420,160,36); add(btnBack);

            btnCalc.addActionListener(e -> calculateTotal());
            btnPay.addActionListener(e -> {
                calculateTotal();
                int response = JOptionPane.showConfirmDialog(this, "Proceed to pay " + lblTotal.getText() + " ?", "Confirm Payment", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    try {
                        // get first hospital as demo if exists
                        List<Hospital> list = db.listHospitals();
                        int hospitalId = list.isEmpty() ? 0 : list.get(0).id;
                        double total = parseMoney(lblTotal.getText());
                        int bookingId = db.createBooking(loggedUserId, lastPatientId, hospitalId, bookedAmbulanceId, null, null, total);
                        lastBookingId = bookingId;
                        showDialog("Payment successful. Booking ID: " + bookingId, "Paid", JOptionPane.INFORMATION_MESSAGE);
                        cardLayout.show(root, "summary");
                    } catch (SQLException ex) {
                        showDialog("Database error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            btnBack.addActionListener(e -> cardLayout.show(root, "wardfood"));
        }

        private void calculateTotal() {
            double doc = parseDouble(tfDoctorFee.getText(), 0);
            String r = (String) cbRoom.getSelectedItem();
            double room = 0;
            if (r.contains("5000")) room = 5000; else if (r.contains("3000")) room = 3000; else if (r.contains("1500")) room = 1500;
            String f = (String) cbFood.getSelectedItem();
            double food = 0;
            if (f.contains("300")) food = 300; else if (f.contains("500")) food = 500; else if (f.contains("250")) food = 250; else if (f.contains("800")) food = 800;
            double amb = cbAmbUsed.isSelected() ? 1500 : 0;
            double misc = parseDouble(tfMisc.getText(), 0);
            double tot = doc + room + food + amb + misc;
            lblTotal.setText(String.format("Total: ₹%.2f", tot));
        }
        private double parseDouble(String s, double def) {
            try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
        }
        private double parseMoney(String s) {
            try { return Double.parseDouble(s.replaceAll("[^0-9.]", "")); } catch (Exception e) { return 0; }
        }
    }

    // ---- Summary/Reviews ----
    private class SummaryPanel extends BackgroundPanel {
        SummaryPanel() {
            super(bgFormImg);
            JLabel title = new JLabel("Summary & Reviews", SwingConstants.CENTER); title.setFont(new Font("SansSerif", Font.BOLD, 26)); title.setForeground(Color.WHITE); title.setBounds(0,20,1100,40); add(title);
            JTextArea ta = new JTextArea(); ta.setEditable(false); ta.setBounds(150, 100, 800, 350); ta.setText("Summary:\nPatient ID: " + lastPatientId + "\nBooking ID: " + (lastBookingId == null ? "N/A" : lastBookingId) + "\n\nPhotos/Video placeholders."); add(ta);
            JComboBox<Integer> stars = new JComboBox<>(new Integer[]{1,2,3,4,5}); stars.setBounds(420,470,80,28); add(stars);
            JTextField tfReview = new JTextField(); tfReview.setBounds(520,470,430,28); add(tfReview);
            JButton btnSubmit = new JButton("Submit Review"); btnSubmit.setBounds(420, 520, 150, 34); add(btnSubmit);
            JButton btnFinish = new JButton("Finish (Back to Login)"); btnFinish.setBounds(590,520,200,34); add(btnFinish);

            btnSubmit.addActionListener(e -> {
                int rating = (int) stars.getSelectedItem();
                String text = tfReview.getText().trim();
                try {
                    db.saveReview(1, rating, text);
                    showDialog("Review submitted. Thank you!", "Thanks", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    showDialog("Database error: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnFinish.addActionListener(e -> {
                loggedUserId = -1; lastPatientId = -1; bookedAmbulanceId = null; lastBookingId = null;
                cardLayout.show(root, "login");
            });
        }
    }

    // -------- helpers --------

    private void showDialog(String message, String title, int messageType) {
        // Modal dialog with X that only closes the dialog
        final JDialog dlg = new JDialog(this, title, true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setSize(420, 160);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(240,240,240));
        JButton close = new JButton("X");
        close.addActionListener(e -> dlg.dispose());
        top.add(close, BorderLayout.EAST);
        dlg.add(top, BorderLayout.NORTH);

        JLabel lbl = new JLabel("<html><div style='text-align:center;'>" + escapeHtml(message) + "</div></html>", SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        dlg.add(lbl, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton ok = new JButton("OK"); ok.addActionListener(e -> dlg.dispose());
        bottom.add(ok);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<","&lt;").replace(">","&gt;").replace("\n", "<br/>");
    }

    // ---------- main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpecializedHospitalManagementClean());
    }
}
