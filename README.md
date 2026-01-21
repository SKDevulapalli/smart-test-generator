# Kade Test Gen

Enterprise-grade test automation code generator that transforms requirements into production-ready test code.

## Features

### Analyze Live Page
Analyze any web page using a headless browser and automatically generate BDD (Behavior-Driven Development) scenarios based on the actual DOM structure.

- **DOM Analysis**: Extracts interactive elements, forms, buttons, and links
- **Smart Flow Detection**: Infers user flows and test scenarios from page structure
- **BDD Generation**: Produces Gherkin-formatted scenarios ready for Cucumber/SpecFlow
- **Edge Case Detection**: Identifies potential edge cases and security considerations
- **Syntax Highlighting**: Beautiful Gherkin syntax highlighting with copy/download support

### Generate Test Code
Create executable test code from requirements documents, GitHub repositories, or direct text input.

- **Multiple Input Sources**:
  - Upload Word documents (.docx, .doc, .txt)
  - Connect to GitHub repositories
  - Direct text/BDD scenario input

- **Test Frameworks Supported**:
  - **UI Tests**: Selenium WebDriver (Java)
  - **API Tests**: REST Assured
  - **Mobile Tests**: Appium

- **Generated Artifacts**:
  - Page Object Model classes
  - Test classes with assertions
  - Utility/helper classes
  - Configuration files

## Tech Stack

- **Backend**: Spring Boot 3.2.1, Java 17
- **Frontend**: Vanilla JavaScript, CSS3
- **DOM Analysis**: Selenium WebDriver with WebDriverManager
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Chrome browser (for headless DOM analysis)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/ai-test-gen.git
cd ai-test-gen
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

4. Open your browser and navigate to:
```
http://localhost:8081
```

### Configuration

The application runs on port 8081 by default. Configure in `application.properties`:

```properties
server.port=8081
```

## Usage

### Analyzing a Live Page

1. Select **Analyze Live Page** mode
2. Enter the URL you want to analyze
3. Click **Analyze**
4. Review the generated BDD scenarios, user flows, and edge cases
5. Copy or download the `.feature` file

### Generating Test Code

1. Select **Generate Test Code** mode
2. Choose your input source:
   - **Upload Document**: Drag & drop or browse for a requirements file
   - **GitHub**: Enter repository URL (with optional access token for private repos)
   - **Direct Input**: Paste requirements or BDD scenarios directly
3. Configure output options:
   - Select test type (UI/API/Mobile)
   - Set base package name
   - Set application URL
4. Click **Generate Tests**
5. Review and download generated code

## Project Structure

```
src/
├── main/
│   ├── java/com/enterprise/testgen/
│   │   ├── controller/       # REST controllers
│   │   ├── model/            # Domain models
│   │   └── service/          # Business logic
│   │       ├── BDDScenarioGeneratorService.java
│   │       ├── WebPageAnalyzerService.java
│   │       ├── TestGeneratorService.java
│   │       └── ...
│   └── resources/
│       ├── static/
│       │   ├── css/          # Stylesheets
│       │   ├── js/           # JavaScript
│       │   └── img/          # Images
│       └── templates/        # Thymeleaf templates
└── test/                     # Unit tests
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Main application page |
| GET | `/docs` | Documentation page |
| GET | `/api-docs` | API documentation |
| POST | `/api/analyze/url` | Analyze a URL and generate BDD scenarios |
| GET | `/api/analyze/details` | Get page analysis details |
| POST | `/api/generate/document` | Generate tests from uploaded document |
| POST | `/api/generate/github` | Generate tests from GitHub repository |
| POST | `/api/generate/text` | Generate tests from text input |
| GET | `/api/health` | Health check endpoint |
| GET | `/api/copilot/status` | Check AI copilot integration status |

## License

This project is proprietary software. All rights reserved.

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting pull requests.

## Support

For issues and feature requests, please use the GitHub issue tracker.

---

Built with enterprise test automation in mind.
