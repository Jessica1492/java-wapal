# Setup

1. Download [Firefox Selenium driver](https://github.com/mozilla/geckodriver)
2. Build the project with `mvn compile`
3. Set environment variables (or pass in next step)

   - `IC_PATH` - the location of the game .html file
   - `webdriver.gecko.driver` - the location of the geckodriver executable

4. Run `mvn exec:java` 
   or `mvn -DIC_PATH="..."" -Dwebdriver.gecko.driver="..." exec:java`
