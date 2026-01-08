# Concordia Community

A Reddit-style community platform built for Concordia University students. This project lets students browse, create, and discuss posts about campus life, courses, and student experiences.

Watch Demo Video

Check out the demo here: https://youtu.be/BZlZUEo13NA

## Features

- **Browse Posts**: View the latest posts from the Concordia community with search and sort functionality
- **User Authentication**: Register and login to create posts, vote, and comment
- **Voting System**: Upvote/downvote posts to highlight the most relevant content
- **Comments**: Engage in discussions by commenting on posts
- **Search & Sort**: Find specific posts or sort by hot, new, or top
- **Post Categorization**: AI-powered categorization using HuggingFace API

## Tech Stack

- **Backend**: Spring Boot 3.5.7 with Java
- **Database**: PostgreSQL (hosted on Neon)
- **Frontend**: Thymeleaf templates
- **Security**: Spring Security with BCrypt password encryption
- **Deployment**: Render (free tier)
- **APIs**: Reddit API for initial data, HuggingFace API for post categorization

## Prerequisites

Before running this locally, you'll need:

- Java 17 or higher
- PostgreSQL database
- Maven
- IDE (IntelliJ, Eclipse, VS Code, etc.)

## Installation

### Backend Setup

1. Clone this repository
2. Open the project in your IDE
3. Create an `application.properties` file in `src/main/resources` with your database credentials:

```properties
spring.datasource.url=jdbc:postgresql://your-database-url
spring.datasource.username=your-username
spring.datasource.password=your-password

# Reddit API (optional, for importing posts)
reddit.client.id=your-client-id
reddit.client.secret=your-client-secret
reddit.user.agent=your-user-agent
reddit.access-token=your-access-token

# HuggingFace API (optional, for categorization)
huggingface.api.key=your-api-key
```

4. Run `./mvnw spring-boot:run`
5. Access the app at `http://localhost:8080`

## Usage

- Visit the homepage to browse posts
- Register an account to create posts and interact with content
- Use the search bar to find specific topics
- Click on posts to view details and comments
- Vote on posts you find helpful or interesting

## API Endpoints

- `/api/reddit/posts` - View all posts
- `/api/reddit/create` - Create a new post (requires login)
- `/api/reddit/vote/{id}/up` - Upvote a post (requires login)
- `/api/reddit/vote/{id}/down` - Downvote a post (requires login)
- `/api/reddit/comment/add` - Add a comment (requires login)

## Contributing

Feel free to submit pull requests or open issues if you find bugs or want to add features!
