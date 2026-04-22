DROP DATABASE IF EXISTS ngo_donation_system;
CREATE DATABASE ngo_donation_system;
USE ngo_donation_system;

-- 1. USER
CREATE TABLE USER (
    user_id     INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    password    VARCHAR(255) NOT NULL,
    address     TEXT,
    city        VARCHAR(100),
    role        ENUM('donor', 'admin', 'volunteer', 'ngo_admin') NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. NGO
CREATE TABLE NGO (
    ngo_id      INT PRIMARY KEY AUTO_INCREMENT,
    ngo_name    VARCHAR(150) NOT NULL,
    address     TEXT,
    city        VARCHAR(100),
    state       VARCHAR(100),
    phone       VARCHAR(20),
    email       VARCHAR(150),
    description TEXT
);

-- 3. VOLUNTEER
CREATE TABLE VOLUNTEER (
    volunteer_id        INT PRIMARY KEY AUTO_INCREMENT,
    user_id             INT NOT NULL,
    ngo_id              INT NOT NULL,
    volunteer_status    ENUM('active', 'inactive', 'pending') NOT NULL,
    joined_date         DATE,
    FOREIGN KEY (user_id) REFERENCES USER(user_id),
    FOREIGN KEY (ngo_id)  REFERENCES NGO(ngo_id)
);

-- 4. CAMPAIGN
CREATE TABLE CAMPAIGN (
    campaign_id         INT PRIMARY KEY AUTO_INCREMENT,
    ngo_id              INT NOT NULL,
    admin_id            INT NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    donation_type       ENUM('monetary', 'goods', 'both') NOT NULL,
    target_amount       DECIMAL(15, 2),
    collected_amount    DECIMAL(15, 2) DEFAULT 0.00,
    start_date          DATE,
    end_date            DATE,
    campaign_status     ENUM('active', 'inactive', 'completed', 'cancelled') NOT NULL,
    FOREIGN KEY (ngo_id)    REFERENCES NGO(ngo_id),
    FOREIGN KEY (admin_id)  REFERENCES USER(user_id)
);

-- 5. URGENT_NEEDS
CREATE TABLE URGENT_NEEDS (
    urgent_id       INT PRIMARY KEY AUTO_INCREMENT,
    admin_id        INT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         TEXT,
    start_time      DATETIME,
    end_time        DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    urgent_status   ENUM('open', 'closed', 'fulfilled') NOT NULL,
    FOREIGN KEY (admin_id) REFERENCES USER(user_id)
);

-- 6. DONATION_REQUEST
CREATE TABLE DONATION_REQUEST (
    request_id      INT PRIMARY KEY AUTO_INCREMENT,
    user_id         INT NOT NULL,
    campaign_id     INT NOT NULL,
    donation_type   ENUM('monetary', 'goods') NOT NULL,
    amount          DECIMAL(15, 2),
    request_message TEXT,
    request_status  ENUM('pending', 'approved', 'rejected') NOT NULL,
    requested_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_by     INT,
    approved_at     DATETIME,
    FOREIGN KEY (user_id)     REFERENCES USER(user_id),
    FOREIGN KEY (campaign_id) REFERENCES CAMPAIGN(campaign_id),
    FOREIGN KEY (approved_by) REFERENCES USER(user_id)
);

-- 7. DONATION
CREATE TABLE DONATION (
    donation_id     INT PRIMARY KEY AUTO_INCREMENT,
    user_id         INT NOT NULL,
    campaign_id     INT NOT NULL,
    donation_type   ENUM('monetary', 'goods') NOT NULL,
    amount          DECIMAL(15, 2),
    donation_status ENUM('pending', 'completed', 'cancelled') NOT NULL,
    donation_date   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)     REFERENCES USER(user_id),
    FOREIGN KEY (campaign_id) REFERENCES CAMPAIGN(campaign_id)
);

-- 8. PAYMENT
CREATE TABLE PAYMENT (
    payment_id      INT PRIMARY KEY AUTO_INCREMENT,
    donation_id     INT NOT NULL,
    payment_method  ENUM('credit_card', 'debit_card', 'upi', 'bank_transfer', 'cash') NOT NULL,
    transaction_id  VARCHAR(100),
    amount          DECIMAL(15, 2) NOT NULL,
    payment_status  ENUM('pending', 'success', 'failed', 'refunded') NOT NULL,
    payment_date    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donation_id) REFERENCES DONATION(donation_id)
);

-- 9. DONATION_ITEM
CREATE TABLE DONATION_ITEM (
    item_id         INT PRIMARY KEY AUTO_INCREMENT,
    donation_id     INT NOT NULL,
    item_name       VARCHAR(150) NOT NULL,
    category        VARCHAR(100),
    quantity        INT NOT NULL DEFAULT 1,
    description     TEXT,
    estimated_value DECIMAL(15, 2),
    FOREIGN KEY (donation_id) REFERENCES DONATION(donation_id)
);

-- 10. PICKUP_REQUEST
CREATE TABLE PICKUP_REQUEST (
    pickup_id       INT PRIMARY KEY AUTO_INCREMENT,
    donation_id     INT NOT NULL,
    donor_address   TEXT NOT NULL,
    pickup_date     DATE NOT NULL,
    time_slot       VARCHAR(50),
    pickup_status   ENUM('pending', 'assigned', 'completed', 'cancelled') NOT NULL,
    FOREIGN KEY (donation_id) REFERENCES DONATION(donation_id)
);

-- 11. DONATION_RECEIPT
CREATE TABLE DONATION_RECEIPT (
    receipt_id      INT PRIMARY KEY AUTO_INCREMENT,
    donation_id     INT NOT NULL,
    receipt_number  VARCHAR(100) NOT NULL UNIQUE,
    issued_date     DATETIME DEFAULT CURRENT_TIMESTAMP,
    certificate_url VARCHAR(255),
    FOREIGN KEY (donation_id) REFERENCES DONATION(donation_id)
);

-- 12. TASK_ASSIGNMENT
CREATE TABLE TASK_ASSIGNMENT (
    task_id         INT PRIMARY KEY AUTO_INCREMENT,
    pickup_id       INT NOT NULL,
    volunteer_id    INT NOT NULL,
    assigned_date   DATETIME DEFAULT CURRENT_TIMESTAMP,
    task_status     ENUM('pending', 'in_progress', 'completed', 'cancelled') NOT NULL,
    FOREIGN KEY (pickup_id)     REFERENCES PICKUP_REQUEST(pickup_id),
    FOREIGN KEY (volunteer_id)  REFERENCES VOLUNTEER(volunteer_id)
);
-- ================================================
-- DUMMY DATA INSERTION SCRIPT (CORRECTED)
-- ================================================

-- 1. USER
INSERT INTO USER (name, email, phone, password, address, city, role, created_at) VALUES
('Poojaa Kumar', 'poojaakumar3110@gmail.com',   '9445307145', 'POOJAA1234', 'No 10/11 south sector 2nd street', 'Chennai','donor',     NOW()),
('Poojaa admin', 'poojaak005@gmail.com',  '9488089945', 'ADMIN1234', '45 Anna Nagar', 'Mumbai', 'admin',     NOW()),
('NGO Admin', 'lkumar2671@gmail.com', '9988776655', 'NGO1234',   '7 Gandhi Street', 'Delhi',     'ngo_admin', NOW()),
('Arulmozhi M',  'arulmozhi3077@gmail.com',    '9001122334', 'ARUL1234',   '3 Lake View',     'Bangalore', 'volunteer', NOW());


-- 2. NGO
INSERT INTO NGO (ngo_name, address, city, state, phone, email, description) VALUES
('HelpIndia Foundation', '22 Nehru Place',  'Delhi',   'Delhi',       '0112233445', 'helpindia@ngo.org',  'Providing food and shelter to the needy.'),
('CareBridge Trust',     '88 Park Street',  'Kolkata', 'West Bengal', '0334455667', 'carebridge@ngo.org', 'Education and healthcare for underprivileged children.');
INSERT INTO NGO (ngo_name, address, city, state, phone, email, description) VALUES
('Chennai Care Foundation', '12 Anna Salai', 'Chennai', 'Tamil Nadu', '0442233445', 'chennaicare@ngo.org', 'Supports homeless people with food and shelter.'),
('Hope for All Trust', '45 T Nagar Main Road', 'Chennai', 'Tamil Nadu', '0445566778', 'hopeforall@ngo.org', 'Provides education support for underprivileged children.');
-- 3. VOLUNTEER
INSERT INTO VOLUNTEER (user_id, ngo_id, volunteer_status, joined_date) VALUES
(4, 1, 'active', '2025-01-15');

-- 4. CAMPAIGN
INSERT INTO CAMPAIGN (ngo_id, admin_id, title, description, donation_type, target_amount, collected_amount, start_date, end_date, campaign_status) VALUES
(1, 3, 'Feed the Hungry',    'Campaign to provide meals to homeless people.',     'monetary', 500000.00, 120000.00, '2025-01-01', '2025-06-30', 'active'),
(2, 3, 'Books for Children', 'Donate books and stationery for rural school kids.', 'goods',   200000.00, 45000.00,  '2025-02-01', '2025-07-31', 'active');

-- 5. URGENT_NEEDS
INSERT INTO URGENT_NEEDS (admin_id, title, message, start_time, end_time, created_at, urgent_status) VALUES
(3, 'Flood Relief Supplies', 'Urgent need for blankets and food packets for flood victims in Bihar.', '2025-03-01 08:00:00', '2025-03-10 20:00:00', NOW(), 'open'),
(3, 'Medical Camp Support',  'Need volunteers and medicines for free medical camp in Delhi slums.',  '2025-03-05 09:00:00', '2025-03-06 18:00:00', NOW(), 'open');

-- 6. DONATION_REQUEST
INSERT INTO DONATION_REQUEST (user_id, campaign_id, donation_type, amount, request_message, request_status, requested_at, approved_by, approved_at) VALUES
(1, 1, 'monetary', 5000.00, 'I would like to donate towards the feeding campaign.', 'approved', NOW(), 2, NOW()),
(1, 2, 'goods', 1000.00, 'I have books and stationery to donate for the campaign.', 'pending', NOW(), NULL, NULL);

-- 7. DONATION
INSERT INTO DONATION (user_id, campaign_id, donation_type, amount, donation_status, donation_date) VALUES
(1, 1, 'monetary', 5000.00, 'completed', NOW()),
(1, 2, 'goods', 1000.00, 'pending', NOW());

select* from donation where user_id=1;
-- 8. PAYMENT
INSERT INTO PAYMENT (donation_id, payment_method, transaction_id, amount, payment_status, payment_date) VALUES
(1, 'upi', 'UPI20250310001', 5000.00, 'success', NOW()),
(2, 'bank_transfer','ID234567897899', 1000.00, 'pending', NOW());

-- 9. DONATION_ITEM
INSERT INTO DONATION_ITEM (donation_id, item_name, category, quantity, description, estimated_value) VALUES
(2, 'Notebooks', 'Stationery', 50, 'A4 size ruled notebooks for students.', 1500.00),
(2, 'Story Books', 'Books', 30, 'English story books for children aged 8-12.', 2000.00);

-- 10. PICKUP_REQUEST
INSERT INTO PICKUP_REQUEST (donation_id, donor_address, pickup_date, time_slot, pickup_status) VALUES
(2, '12 MG Road, Chennai', '2025-03-15', '10:00 AM - 12:00 PM', 'pending');

-- 11. DONATION_RECEIPT
INSERT INTO DONATION_RECEIPT (donation_id, receipt_number, issued_date, certificate_url) VALUES
(1, 'RCPT-2025-0001', NOW(), 'https://receipts.helpindia.org/RCPT-2025-0001.pdf'),
(2, 'RCPT-2025-0002', NOW(), 'https://receipts.carebridge.org/RCPT-2025-0002.pdf');

-- 12. TASK_ASSIGNMENT
INSERT INTO TASK_ASSIGNMENT (pickup_id, volunteer_id, assigned_date, task_status) VALUES
(1, 1, NOW(), 'pending');
