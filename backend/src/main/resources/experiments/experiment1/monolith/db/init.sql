-- Seed data for RealWorld blog application

-- Users
INSERT INTO public.users (id, username, password, email, bio, image) VALUES
('user-001', 'john_doe', '$2a$12$5MSCsIhU.KynyvMPCMdTZuiaH6fKx/dzU8BBBRNHoQVf1OncZ1rVK', 'john@example.com', 'Software developer and tech enthusiast', 'https://api.dicebear.com/7.x/avataaars/svg?seed=john'),
('user-002', 'jane_smith', '$2a$12$5MSCsIhU.KynyvMPCMdTZuiaH6fKx/dzU8BBBRNHoQVf1OncZ1rVK', 'jane@example.com', 'Full-stack developer passionate about clean code', 'https://api.dicebear.com/7.x/avataaars/svg?seed=jane'),
('user-003', 'bob_wilson', '$2a$12$5MSCsIhU.KynyvMPCMdTZuiaH6fKx/dzU8BBBRNHoQVf1OncZ1rVK', 'bob@example.com', 'DevOps engineer and cloud architect', 'https://api.dicebear.com/7.x/avataaars/svg?seed=bob'),
('user-004', 'alice_brown', '$2a$12$5MSCsIhU.KynyvMPCMdTZuiaH6fKx/dzU8BBBRNHoQVf1OncZ1rVK', 'alice@example.com', 'Data scientist exploring ML and AI', 'https://api.dicebear.com/7.x/avataaars/svg?seed=alice'),
('user-005', 'charlie_davis', '$2a$12$5MSCsIhU.KynyvMPCMdTZuiaH6fKx/dzU8BBBRNHoQVf1OncZ1rVK', 'charlie@example.com', 'Frontend developer and UX enthusiast', 'https://api.dicebear.com/7.x/avataaars/svg?seed=charlie');

-- Tags
INSERT INTO public.tags (id, name) VALUES
('tag-001', 'kotlin'),
('tag-002', 'spring-boot'),
('tag-003', 'microservices'),
('tag-004', 'docker'),
('tag-005', 'kubernetes'),
('tag-006', 'react'),
('tag-007', 'typescript'),
('tag-008', 'postgresql'),
('tag-009', 'testing'),
('tag-010', 'devops');

-- Articles
INSERT INTO public.articles (id, user_id, slug, title, description, body, created_at, updated_at) VALUES
('article-001', 'user-001', 'getting-started-with-kotlin', 'Getting Started with Kotlin', 'A beginner guide to Kotlin programming', 'Kotlin is a modern programming language that makes developers happier. It is concise, safe, and fully interoperable with Java. In this article, we will explore the basics of Kotlin and why it has become so popular for Android and server-side development.', '2024-01-15 10:00:00', '2024-01-15 10:00:00'),
('article-002', 'user-001', 'spring-boot-best-practices', 'Spring Boot Best Practices', 'Tips for building production-ready Spring Boot applications', 'Spring Boot has revolutionized Java development by providing sensible defaults and auto-configuration. This article covers essential best practices for building scalable and maintainable Spring Boot applications.', '2024-01-20 14:30:00', '2024-01-20 14:30:00'),
('article-003', 'user-002', 'microservices-architecture-patterns', 'Microservices Architecture Patterns', 'Common patterns for designing microservices', 'Microservices architecture has become the de facto standard for building large-scale distributed systems. This article explores common patterns like API Gateway, Circuit Breaker, and Service Mesh.', '2024-02-01 09:00:00', '2024-02-05 11:00:00'),
('article-004', 'user-002', 'docker-for-developers', 'Docker for Developers', 'Essential Docker concepts every developer should know', 'Docker containers have transformed how we develop, ship, and run applications. Learn the fundamental concepts of containerization and how to use Docker effectively in your development workflow.', '2024-02-10 16:00:00', '2024-02-10 16:00:00'),
('article-005', 'user-003', 'kubernetes-deployment-strategies', 'Kubernetes Deployment Strategies', 'Blue-green, canary, and rolling deployments in K8s', 'Deploying applications to Kubernetes can be done in several ways. This article compares different deployment strategies and when to use each one for zero-downtime deployments.', '2024-02-15 08:00:00', '2024-02-15 08:00:00'),
('article-006', 'user-003', 'infrastructure-as-code-terraform', 'Infrastructure as Code with Terraform', 'Managing cloud resources declaratively', 'Infrastructure as Code (IaC) allows you to manage and provision infrastructure through code. Terraform is one of the most popular tools for this purpose.', '2024-02-20 13:00:00', '2024-02-20 13:00:00'),
('article-007', 'user-004', 'react-hooks-deep-dive', 'React Hooks Deep Dive', 'Understanding useState, useEffect, and custom hooks', 'React Hooks changed how we write React components. This deep dive covers the most important hooks and how to create your own custom hooks for reusable logic.', '2024-03-01 10:00:00', '2024-03-01 10:00:00'),
('article-008', 'user-004', 'typescript-advanced-types', 'TypeScript Advanced Types', 'Mastering generics, conditional types, and mapped types', 'TypeScript provides powerful type system features beyond basic types. Learn how to leverage advanced types to write safer and more expressive code.', '2024-03-05 15:00:00', '2024-03-05 15:00:00'),
('article-009', 'user-005', 'postgresql-performance-tuning', 'PostgreSQL Performance Tuning', 'Optimizing queries and database configuration', 'PostgreSQL is a powerful database, but it requires proper tuning for optimal performance. This article covers indexing strategies, query optimization, and configuration parameters.', '2024-03-10 11:00:00', '2024-03-10 11:00:00'),
('article-010', 'user-005', 'testing-strategies-for-microservices', 'Testing Strategies for Microservices', 'Unit, integration, and contract testing approaches', 'Testing microservices presents unique challenges. Learn about the testing pyramid, contract testing with Pact, and strategies for end-to-end testing in distributed systems.', '2024-03-15 09:00:00', '2024-03-15 09:00:00');

-- Article Tags
INSERT INTO public.article_tags (article_id, tag_id) VALUES
('article-001', 'tag-001'),
('article-002', 'tag-001'),
('article-002', 'tag-002'),
('article-003', 'tag-003'),
('article-003', 'tag-004'),
('article-004', 'tag-004'),
('article-005', 'tag-005'),
('article-005', 'tag-010'),
('article-006', 'tag-010'),
('article-007', 'tag-006'),
('article-007', 'tag-007'),
('article-008', 'tag-007'),
('article-009', 'tag-008'),
('article-010', 'tag-009'),
('article-010', 'tag-003');

-- Follows (who follows whom)
INSERT INTO public.follows (user_id, follow_id) VALUES
('user-001', 'user-002'),
('user-001', 'user-003'),
('user-002', 'user-001'),
('user-002', 'user-004'),
('user-003', 'user-001'),
('user-003', 'user-002'),
('user-004', 'user-005'),
('user-005', 'user-001'),
('user-005', 'user-002'),
('user-005', 'user-003');

-- Article Favorites
INSERT INTO public.article_favorites (article_id, user_id) VALUES
('article-001', 'user-002'),
('article-001', 'user-003'),
('article-001', 'user-004'),
('article-002', 'user-003'),
('article-002', 'user-005'),
('article-003', 'user-001'),
('article-003', 'user-004'),
('article-003', 'user-005'),
('article-004', 'user-001'),
('article-005', 'user-001'),
('article-005', 'user-002'),
('article-006', 'user-004'),
('article-007', 'user-001'),
('article-007', 'user-003'),
('article-008', 'user-002'),
('article-009', 'user-003'),
('article-010', 'user-001'),
('article-010', 'user-002');

-- Comments
INSERT INTO public.comments (id, body, article_id, user_id, created_at, updated_at) VALUES
('comment-001', 'Great introduction to Kotlin! Very helpful for beginners.', 'article-001', 'user-002', '2024-01-16 09:00:00', '2024-01-16 09:00:00'),
('comment-002', 'I wish you had covered coroutines as well.', 'article-001', 'user-003', '2024-01-17 14:00:00', '2024-01-17 14:00:00'),
('comment-003', 'These best practices saved me hours of debugging!', 'article-002', 'user-004', '2024-01-21 10:00:00', '2024-01-21 10:00:00'),
('comment-004', 'Excellent article on microservices patterns.', 'article-003', 'user-001', '2024-02-02 11:00:00', '2024-02-02 11:00:00'),
('comment-005', 'Could you write a follow-up on event sourcing?', 'article-003', 'user-005', '2024-02-03 16:00:00', '2024-02-03 16:00:00'),
('comment-006', 'Docker has changed how I develop applications.', 'article-004', 'user-003', '2024-02-11 08:00:00', '2024-02-11 08:00:00'),
('comment-007', 'The Kubernetes deployment strategies comparison is very useful.', 'article-005', 'user-001', '2024-02-16 10:00:00', '2024-02-16 10:00:00'),
('comment-008', 'I prefer ArgoCD for GitOps deployments.', 'article-005', 'user-004', '2024-02-17 12:00:00', '2024-02-17 12:00:00'),
('comment-009', 'React hooks simplified my component logic significantly.', 'article-007', 'user-005', '2024-03-02 09:00:00', '2024-03-02 09:00:00'),
('comment-010', 'TypeScript generics were confusing until I read this article.', 'article-008', 'user-001', '2024-03-06 11:00:00', '2024-03-06 11:00:00'),
('comment-011', 'The indexing tips improved our query performance by 10x.', 'article-009', 'user-002', '2024-03-11 14:00:00', '2024-03-11 14:00:00'),
('comment-012', 'Contract testing with Pact is a game changer.', 'article-010', 'user-004', '2024-03-16 10:00:00', '2024-03-16 10:00:00');
