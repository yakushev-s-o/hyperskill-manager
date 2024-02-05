# HS-Manager

HS-Manager is a tool designed to automate interactions with the educational platform [hyperskill.org](https://hyperskill.org).

## Key Features

- **Data Collection:** Automatically extracts information about available topics, projects, project stages, and assignments.
- **Page Saving:** Enables saving of pages, including topics, projects, and project stages, while maintaining the site's hierarchical structure.
- **Answer Retrieval:** Capable of reading answers to previously solved tests.
- **Answer Submission:** Automates the test-taking process based on the collected data.

## Configuration

Edit `settings.json` for the program to work correctly:

```json
{
  "login": "YOUR_LOGIN",
  "password": "YOUR_PASSWORD",
  "chromedriver_path": "C:/tools/chromedriver_win32/chromedriver.exe",
  "folder_path": "C:/Users/Admin/Desktop/track/TRACK_NUMBER/",
  "json_path": "src/main/resources/answer-list-TRACK_NUMBER.json",
  "data_path": "src/main/resources/data-list-TRACK_NUMBER.json",
  "site_link": "https://hyperskill.org/"
}
```
Fill in your login and password, adjust paths as needed. **Do not change "TRACK_NUMBER"**; it's a required placeholder for the program's operation.

## Requirements

To use this program, you must have `chromedriver` installed. `chromedriver` is essential for automating web browser interactions, and it must be compatible with the version of Chrome installed on your system. Ensure that `chromedriver` is correctly set up and accessible in your system's PATH.

For installation instructions and downloads, please visit the [ChromeDriver - WebDriver for Chrome](https://chromedriver.chromium.org/) page.


## Disclaimer

This project is developed solely for the purpose of automating and simplifying processes that users could manually perform on hyperskill.org. It is intended for educational use and should not be used to violate the site's usage policies, copyright laws, or any other legal restrictions.

The developers are not responsible for the use of this software or for any consequences that may arise from such use. By using this software, users assume full responsibility for complying with all applicable laws and the terms of use of hyperskill.org.
