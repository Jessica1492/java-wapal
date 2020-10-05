# Setup

1. Download [Firefox Selenium driver](https://github.com/mozilla/geckodriver)
2. Build the project with `mvn compile`
3. Set environment variables (or pass in next step)

   - `IC_PATH` - the location of the game .html file, e.g. `"file:///home/wape/game/ic110/Incubus%20City%20v1.10.4.html"`
   - `webdriver.gecko.driver` - the location of the geckodriver executable, e.g. `"/home/wape/java-wapal/geckodriver"`

4. Run `mvn exec:java` 
   or `mvn -DIC_PATH="..." -Dwebdriver.gecko.driver="..." exec:java`
   e.g. `mvn -Dwebdriver.gecko.driver="/home/wape/java-wapal/geckodriver" -DIC_PATH="file:///home/wape/game/ic110/Incubus%20City%20v1.10.4.html" exec:java`