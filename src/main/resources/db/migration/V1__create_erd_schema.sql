CREATE TABLE member (
  id INT AUTO_INCREMENT PRIMARY KEY,
  login_id VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  name VARCHAR(10) NOT NULL,
  email VARCHAR(100) NOT NULL,
  birth_date DATE NOT NULL,
  region VARCHAR(100),
  codef_connected_id VARCHAR(100),
  salary_amount NUMERIC(14,2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE goal (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  monthly_salary INT NOT NULL,
  saving_target INT NOT NULL,
  daily_budget INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME,
  CONSTRAINT fk_goal_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE region_code (
  id INT AUTO_INCREMENT PRIMARY KEY,
  parent_id INT,
  region_code VARCHAR(30) NOT NULL UNIQUE,
  province VARCHAR(50) NOT NULL,
  city VARCHAR(50),
  region_level VARCHAR(30) NOT NULL,
  CONSTRAINT fk_region_parent FOREIGN KEY (parent_id) REFERENCES region_code(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  source_policy_id VARCHAR(100) NOT NULL UNIQUE,
  source_type VARCHAR(50) NOT NULL,
  agency_name VARCHAR(100) NOT NULL,
  category ENUM('일자리','주거','교육','복지','금융','창업','문화','생활지원','건강','돌봄','기타') NOT NULL,
  summary VARCHAR(500),
  official_url VARCHAR(500),
  start_date DATE,
  due_date DATE,
  is_always_open BOOLEAN NOT NULL,
  is_active BOOLEAN NOT NULL,
  status VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_condition (
  id INT AUTO_INCREMENT PRIMARY KEY,
  policy_id INT NOT NULL UNIQUE,
  min_age INT,
  max_age INT,
  employment_status VARCHAR(50),
  student_status BOOLEAN,
  income_condition VARCHAR(200),
  condition_summary VARCHAR(500),
  need_check BOOLEAN NOT NULL,
  CONSTRAINT fk_policy_condition_policy FOREIGN KEY (policy_id) REFERENCES policy(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_region (
  id INT AUTO_INCREMENT PRIMARY KEY,
  policy_id INT NOT NULL,
  region_id INT NOT NULL,
  CONSTRAINT fk_policy_region_policy FOREIGN KEY (policy_id) REFERENCES policy(id) ON DELETE CASCADE,
  CONSTRAINT fk_policy_region_region FOREIGN KEY (region_id) REFERENCES region_code(id) ON DELETE RESTRICT,
  CONSTRAINT uk_policy_region UNIQUE (policy_id, region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_policy_profile (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL UNIQUE,
  residence_region_id INT,
  income_range VARCHAR(50),
  employment_status VARCHAR(50),
  student_status BOOLEAN,
  interest_categories VARCHAR(300),
  CONSTRAINT fk_user_policy_profile_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_policy_profile_region FOREIGN KEY (residence_region_id) REFERENCES region_code(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_bookmark (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  policy_id INT NOT NULL,
  apply_status VARCHAR(50) NOT NULL,
  notification_enabled BOOLEAN NOT NULL,
  note VARCHAR(500),
  CONSTRAINT fk_policy_bookmark_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_policy_bookmark_policy FOREIGN KEY (policy_id) REFERENCES policy(id) ON DELETE CASCADE,
  CONSTRAINT uk_policy_bookmark UNIQUE (member_id, policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_calendar_event (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  policy_id INT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  event_date DATE NOT NULL,
  title VARCHAR(200) NOT NULL,
  CONSTRAINT fk_policy_calendar_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_policy_calendar_policy FOREIGN KEY (policy_id) REFERENCES policy(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_notification (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  bookmark_id INT NOT NULL,
  policy_id INT NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  scheduled_at DATETIME NOT NULL,
  is_sent BOOLEAN NOT NULL,
  sent_at DATETIME,
  CONSTRAINT fk_policy_notification_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_policy_notification_bookmark FOREIGN KEY (bookmark_id) REFERENCES policy_bookmark(id) ON DELETE CASCADE,
  CONSTRAINT fk_policy_notification_policy FOREIGN KEY (policy_id) REFERENCES policy(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE savings_product (
  id INT AUTO_INCREMENT PRIMARY KEY,
  product_type VARCHAR(20) NOT NULL,
  product_code VARCHAR(20) NOT NULL,
  company_name VARCHAR(30) NOT NULL,
  product_name VARCHAR(30) NOT NULL,
  join_way VARCHAR(10),
  join_restrict VARCHAR(1),
  join_target VARCHAR(100),
  special_condition VARCHAR(10),
  maturity_interest VARCHAR(20),
  max_amount INT,
  open_date VARCHAR(8),
  close_date VARCHAR(8),
  is_online BOOLEAN,
  difficulty_tag VARCHAR(10),
  is_youth_friendly BOOLEAN,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE savings_product_option (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id2 INT,
  rate_type_code VARCHAR(1),
  rate_type_name VARCHAR(10),
  term_month INT NOT NULL,
  base_rate DECIMAL(5,2),
  max_rate DECIMAL(5,2),
  reserve_type_code VARCHAR(1),
  reserve_type_name VARCHAR(20),
  CONSTRAINT fk_savings_option_product FOREIGN KEY (id2) REFERENCES savings_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pension_product (
  id INT AUTO_INCREMENT PRIMARY KEY,
  product_code VARCHAR(20) NOT NULL,
  company_name VARCHAR(30) NOT NULL,
  product_name VARCHAR(30) NOT NULL,
  join_way VARCHAR(10),
  pension_kind_code VARCHAR(5) NOT NULL,
  pension_kind_name VARCHAR(30) NOT NULL,
  product_type_code VARCHAR(5),
  product_type_name VARCHAR(30),
  avg_profit_rate DECIMAL(6,2),
  profit_rate_1yr DECIMAL(6,2),
  profit_rate_2yr DECIMAL(6,2),
  profit_rate_3yr DECIMAL(6,2),
  sale_company VARCHAR(30),
  open_date VARCHAR(8),
  close_date VARCHAR(8),
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pension_product_option (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id2 INT,
  receive_period_code VARCHAR(5),
  receive_period_name VARCHAR(20),
  entry_age INT,
  monthly_payment VARCHAR(5),
  payment_period INT,
  pension_start_age INT,
  pension_receive_amount INT,
  CONSTRAINT fk_pension_option_product FOREIGN KEY (id2) REFERENCES pension_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE loan_product (
  id INT AUTO_INCREMENT PRIMARY KEY,
  product_type VARCHAR(20) NOT NULL,
  product_code VARCHAR(20) NOT NULL,
  company_name VARCHAR(30) NOT NULL,
  product_name VARCHAR(30) NOT NULL,
  join_way VARCHAR(10),
  loan_limit VARCHAR(50),
  early_repay_fee VARCHAR(100),
  special_condition VARCHAR(10),
  maturity_interest VARCHAR(20),
  note VARCHAR(300),
  max_amount INT,
  delay_rate VARCHAR(10),
  difficulty_tag VARCHAR(300),
  open_date VARCHAR(8),
  close_date VARCHAR(8),
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE loan_product_option (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id2 INT,
  mortgage_type_code VARCHAR(5),
  mortgage_type_name VARCHAR(20),
  repay_type_code VARCHAR(5),
  repay_type_name VARCHAR(20),
  rate_type_code VARCHAR(5),
  rate_type_name VARCHAR(20),
  rate_min DECIMAL(5,2),
  rate_max DECIMAL(5,2),
  rate_avg DECIMAL(5,2),
  credit_grade_section VARCHAR(5),
  CONSTRAINT fk_loan_option_product FOREIGN KEY (id2) REFERENCES loan_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_bookmark (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  product_id INT NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_product_bookmark_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_recommendation (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  product_id INT NOT NULL,
  query_text VARCHAR(100),
  score DECIMAL(5,2),
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_product_recommendation_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE financial_profile (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id2 INT,
  id3 INT,
  risk_type VARCHAR(10),
  preferred_period VARCHAR(10) NOT NULL,
  monthly_deposit INT NOT NULL,
  accept_condition BOOLEAN NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_financial_profile_member FOREIGN KEY (id2) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_financial_profile_goal FOREIGN KEY (id3) REFERENCES goal(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE surplus_fund (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  goal_id INT NOT NULL,
  amount VARCHAR(10) NOT NULL,
  occurred_at DATE NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_surplus_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_surplus_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE goal_history (
  id INT AUTO_INCREMENT PRIMARY KEY,
  member_id INT NOT NULL,
  goal_id INT NOT NULL,
  total_spent INT NOT NULL,
  saving_achieved_amount INT NOT NULL,
  comparison_rate DECIMAL(5,2),
  category_summary_json JSON,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_goal_history_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_goal_history_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `budget` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  `year_month` CHAR(7) NOT NULL,
  salary_amount NUMERIC(14,2) NOT NULL,
  savings_goal_amount NUMERIC(14,2) NOT NULL,
  confirmed_fixed_expense_total NUMERIC(14,2) NOT NULL,
  expected_fixed_expense_total NUMERIC(14,2) NOT NULL,
  available_amount NUMERIC(14,2) NOT NULL,
  daily_recommended_amount NUMERIC(14,2) NOT NULL,
  calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE category (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type ENUM('FIXED','HABIT','PENDING') NOT NULL,
  weight NUMERIC(5,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cards (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  card_name VARCHAR(100) NOT NULL,
  card_number_masked VARCHAR(10) NOT NULL,
  CONSTRAINT fk_cards_member FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merchant_alias (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  default_category_id INT,
  canonical_service_name VARCHAR(255) NOT NULL,
  CONSTRAINT fk_merchant_alias_category FOREIGN KEY (default_category_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merchant (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  merchant_alias_id BIGINT NOT NULL,
  normalized_name VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  CONSTRAINT fk_merchant_alias FOREIGN KEY (merchant_alias_id) REFERENCES merchant_alias(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE card_transaction (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  card_id INT NOT NULL,
  merchant_id BIGINT,
  original_transaction_id INT,
  goal_id INT NOT NULL,
  category_id INT,
  approval_no VARCHAR(50),
  used_date DATE,
  used_at TIMESTAMP,
  source ENUM('CODEF','MANUAL'),
  merchant_name_raw VARCHAR(255),
  merchant_type_raw VARCHAR(100),
  address VARCHAR(255),
  amount NUMERIC(14,2) NOT NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'KRW',
  payment_method VARCHAR(50),
  expected_payment_date DATE,
  industry_code VARCHAR(50),
  installment_months SMALLINT,
  status ENUM('APPROVED','CANCELED','REJECTED','PARTIAL_CANCELED') NOT NULL,
  exchange_rate DECIMAL(10,4),
  card_org_code VARCHAR(10),
  memo VARCHAR(255),
  CONSTRAINT fk_transaction_member FOREIGN KEY (user_id) REFERENCES member(id) ON DELETE CASCADE,
  CONSTRAINT fk_transaction_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
  CONSTRAINT fk_transaction_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id) ON DELETE SET NULL,
  CONSTRAINT fk_transaction_goal FOREIGN KEY (goal_id) REFERENCES goal(id) ON DELETE CASCADE,
  CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE merchant_alias_terms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  merchant_alias_id BIGINT NOT NULL,
  member_id INT NOT NULL,
  alias_text VARCHAR(255) NOT NULL,
  CONSTRAINT fk_alias_terms_alias FOREIGN KEY (merchant_alias_id) REFERENCES merchant_alias(id) ON DELETE CASCADE,
  CONSTRAINT fk_alias_terms_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recurring_payment_group (
  id INT AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  periodicity ENUM('MONTHLY','WEEKLY') NOT NULL,
  repeat_count SMALLINT NOT NULL,
  avg_amount NUMERIC(14,2) NOT NULL,
  amount_variance_pct NUMERIC(5,2),
  avg_pay_day SMALLINT,
  pay_day_variance SMALLINT,
  first_detected_at DATE NOT NULL,
  last_detected_at DATE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fixed_expense_candidate (
  id INT AUTO_INCREMENT PRIMARY KEY,
  recurring_group_id INT NOT NULL,
  recommended_category_id INT NOT NULL,
  score NUMERIC(6,2) NOT NULL,
  recommend_message TEXT,
  status ENUM('PENDING','REGISTERED','EXCLUDED_THIS_MONTH','DO_NOT_RECOMMEND','CLASSIFIED_HABIT') NOT NULL,
  CONSTRAINT fk_fixed_candidate_group FOREIGN KEY (recurring_group_id) REFERENCES recurring_payment_group(id) ON DELETE CASCADE,
  CONSTRAINT fk_fixed_candidate_category FOREIGN KEY (recommended_category_id) REFERENCES category(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fixed_expense (
  id INT AUTO_INCREMENT PRIMARY KEY,
  candidate_id INT,
  name VARCHAR(100) NOT NULL,
  category_id INT,
  expected_pay_day SMALLINT,
  expected_amount NUMERIC(14,2) NOT NULL,
  card_id INT,
  merchant_id BIGINT,
  status ENUM('ACTIVE','CANCELED') NOT NULL,
  CONSTRAINT fk_fixed_candidate FOREIGN KEY (candidate_id) REFERENCES fixed_expense_candidate(id) ON DELETE SET NULL,
  CONSTRAINT fk_fixed_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
  CONSTRAINT fk_fixed_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recurring_payment_group_transaction (
  recurring_group_id INT NOT NULL,
  transaction_id BIGINT NOT NULL,
  PRIMARY KEY (recurring_group_id, transaction_id),
  CONSTRAINT fk_rpgt_group FOREIGN KEY (recurring_group_id) REFERENCES recurring_payment_group(id) ON DELETE CASCADE,
  CONSTRAINT fk_rpgt_transaction FOREIGN KEY (transaction_id) REFERENCES card_transaction(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE habit_expense (
  id INT AUTO_INCREMENT PRIMARY KEY,
  candidate_id INT NOT NULL,
  merchant_id BIGINT NOT NULL,
  category_id INT NOT NULL,
  periodicity ENUM('MONTHLY','WEEKLY') NOT NULL,
  avg_amount NUMERIC(14,2),
  CONSTRAINT fk_habit_candidate FOREIGN KEY (candidate_id) REFERENCES fixed_expense_candidate(id) ON DELETE CASCADE,
  CONSTRAINT fk_habit_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id) ON DELETE CASCADE,
  CONSTRAINT fk_habit_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fixed_expense_id INT,
  type ENUM('PAYMENT_DUE','AMOUNT_CHANGE','MISSED_PAYMENT','RE_REGISTER_SUGGESTION') NOT NULL,
  message TEXT,
  status ENUM('PENDING','SENT','FAILED') NOT NULL,
  scheduled_at TIMESTAMP NOT NULL,
  sent_at TIMESTAMP,
  CONSTRAINT fk_notification_fixed FOREIGN KEY (fixed_expense_id) REFERENCES fixed_expense(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_merchant_preferences (
  id INT AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  category_id INT NOT NULL,
  preference_type ENUM('DO_NOT_RECOMMEND','RECLASSIFY_HABIT','CATEGORY_OVERRIDE') NOT NULL,
  CONSTRAINT fk_user_merchant_preference_merchant FOREIGN KEY (merchant_id) REFERENCES merchant(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_merchant_preference_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
