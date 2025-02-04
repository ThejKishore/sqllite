-- Create a users table
CREATE TABLE users (
                       user_id SERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL,
                       email VARCHAR(100) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create a posts table
CREATE TABLE posts (
                       post_id SERIAL PRIMARY KEY,
                       user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
                       title VARCHAR(200) NOT NULL,
                       content TEXT,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create a comments table
CREATE TABLE comments (
                          comment_id SERIAL PRIMARY KEY,
                          post_id INT REFERENCES posts(post_id) ON DELETE CASCADE,
                          user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
                          content TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (username, email)
VALUES
    ('alice', 'alice@example.com'),
    ('bob', 'bob@example.com'),
    ('charlie', 'charlie@example.com');

-- Insert posts
INSERT INTO posts (user_id, title, content)
VALUES
    (1, 'Post 1 by Alice', 'This is the content of Alices first post.'),
  (2, 'Post 2 by Bob', 'This is the content of Bobs first post.'),
    (3, 'Post 3 by Charlie', 'This is the content of Charlies first post.');

-- Insert comments
INSERT INTO comments (post_id, user_id, content)
VALUES
  (1, 2, 'Great post, Alice!'),
  (1, 3, 'I agree with Bob. Very interesting.'),
  (2, 1, 'Nice post, Bob! Looking forward to more.');