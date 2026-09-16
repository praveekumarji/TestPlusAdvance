INSERT IGNORE INTO education_classes (code, name, active)
VALUES

('CLASS_10', 'BSEB 10th(Matric)', true),
('CLASS_12', 'BSEB 12th(Inter)', true)
;

INSERT IGNORE INTO subscription_plans (id, plan_code, title, subtitle, duration_days, original_amount_in_paise, discounted_amount_in_paise, display_price, display_original_price, discount_percentage, badge, is_recommended)
VALUES
('plan_annual', 'ANNUAL_SUPER_PASS', 'Annual Super Pass', 'Best value for full exam cycle', 210, 9900, 5900, '₹59', '₹99', 40, 'BEST VALUE', true),
('prime_plan_annual', 'ANNUAL_PRIME_PASS', 'Annual prime Pass', 'Best value for full exam cycle', 210, 9900, 7900, '₹79', '₹99', 20, 'BEST VALUE', true);

INSERT IGNORE INTO subscription_plan_features (plan_id, feature)
VALUES
('plan_annual', 'Unlimited Full-Length Mock Tests'),
('plan_annual', 'Weak Area Deep Analytics'),
('plan_annual', 'Step-by-Step Solutions'),
('plan_annual', 'Custom Test Generator Access'),
('plan_annual', 'AI curreted Question Bank'),
('plan_annual', 'VVIP question bank access'),
('plan_annual', 'Previous Year Question Papers with AI based analysis'),
('prime_plan_annual', 'Unlimited Full-Length Mock Tests'),
('prime_plan_annual', 'Weak Area Deep Analytics'),
('prime_plan_annual', 'Step-by-Step Solutions'),
('prime_plan_annual', 'Custom Test Generator Access'),
('prime_plan_annual', 'AI curreted Question Bank'),
('prime_plan_annual', 'VVIP question bank access'),
('prime_plan_annual', 'Previous Year Question Papers with AI based analysis');


INSERT IGNORE INTO subscription_plan_classes (plan_id, class_id)
values('plan_annual',1),('prime_plan_annual',2);


INSERT IGNORE INTO coupons (code, title, discount_type, discount_value, minimum_amount, valid_from, valid_to, is_active, max_uses, applicable_plan_id, created_at)
VALUES
('WELCOME05', 'Welcome Offer', 'FIXED', 5, 0, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), true, 1000, null, NOW()),
('TOPPER10', 'Welcome Offer', 'PERCENTAGE', 50, 25000, NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY), true, 500, 'plan_annual', NOW());

INSERT INTO `user` (`has_used_trial`, `is_email_verified`, `class_id`, `created_at`, `id`, `last_login_at`, `subscription_class_id`, `subscription_expiry`, `auth_provider`, `avatar_url`, `class1_subscription_status`, `class2_subscription_status`, `email`, `full_name`, `google_subject`, `mobile_number`, `password_hash`, `preferred_language`, `subscription_plan`, `target_exam`, `role`, `subscription_status`) VALUES
(b'1', b'0', 2, '2026-09-14 16:20:43.000000', 1, NULL, NULL, '2026-09-17 16:20:43.000000', 'LOCAL', NULL, 'TRIAL', 'TRIAL', 'krpraveen1902@gmail.com', 'Praveen Kumar', NULL, '8757641080', '$2a$10$ysLZ1OGXvj791wNki58eLuLVWwPFjlV6udPwGtNNx5.LQOpjyFAcK', 'en', 'TRIAL_PLAN', NULL, 'ADMIN', 'TRIAL');