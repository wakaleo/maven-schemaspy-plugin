DROP TABLE IF EXISTS salesorder;

DROP TABLE IF EXISTS customer;

DROP TABLE IF EXISTS address;

DROP TABLE IF EXISTS item;

DROP TABLE IF EXISTS product_range;

DROP TABLE IF EXISTS employee;

CREATE TABLE employee
  (
     employee_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
     firstname   VARCHAR(256),
     lastname    VARCHAR(256),
     address     VARCHAR(256),
     city        VARCHAR(256),
     postcode    VARCHAR(256),
     salary      NUMERIC(10, 2),
     company_id  INTEGER,
     INDEX (employee_id)
  );

CREATE TABLE product_range
  (
     product_range_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
     name             VARCHAR(256),
     INDEX (product_range_id)
  );

CREATE TABLE address
  (
     address_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
     address    VARCHAR(40),
     street     VARCHAR(40),
     city       VARCHAR(25),
     state      CHAR(2),
     zipcode    VARCHAR(10),
     country    VARCHAR(20),
     INDEX (address_id)
  );

CREATE TABLE item
  (
     item_id          INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
     name             VARCHAR(256),
     price            NUMERIC(10, 2),
     product_range_id INTEGER,
     CONSTRAINT FOREIGN KEY (product_range_id) REFERENCES product_range(product_range_id)
  );

CREATE TABLE customer
  (
     customer_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
     name        VARCHAR(30),
     telephone   VARCHAR(20),
     address_id  INTEGER,
     INDEX (customer_id),
     FOREIGN KEY (address_id) REFERENCES address(address_id)
  );

CREATE TABLE salesorder
  (
     order_id    INTEGER PRIMARY KEY,
     customer_id INTEGER,
     employee_id INTEGER,
     item_id     INTEGER,
     order_date  DATE,
     ship_date   DATE,
     payment     NUMERIC(10, 2),
     FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
     FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
     FOREIGN KEY (item_id) REFERENCES item(item_id)
  );