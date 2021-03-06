# About

Selenium-automated exhaustive search for high-score in Incubus City.

To be able to search exhaustively,
you must patch your `Incubus City v1.10.4.html`
by changing line 21393
from `&lt;&lt;if random(10)&lt;$incubusXP+8 &gt;&gt;`
to e.g. `&lt;&lt;if random(0)&lt;$incubusXP+8 &gt;&gt;`.
This will show all possible choices deterministically.

# Setup

1. Download [Firefox Selenium driver](https://github.com/mozilla/geckodriver)
2. Build the project with `mvn compile`
3. Set environment variables (or pass as cmdline-arg in next step)

   - `IC_PATH` - the location of the game `.html` file, e.g. `"file:///home/wape/game/ic110/Incubus%20City%20v1.10.4.html"`
   - `webdriver.gecko.driver` - the location of the geckodriver executable, e.g. `"/home/wape/java-wapal/geckodriver"`

4. Run `mvn exec:java` 
   or `mvn -DIC_PATH="..." -Dwebdriver.gecko.driver="..." exec:java`
   e.g. `mvn -Dwebdriver.gecko.driver="/home/wape/java-wapal/geckodriver" -DIC_PATH="file:///home/wape/game/ic110/Incubus%20City%20v1.10.4.html" exec:java`

This will run the Selenium session on your local desktop.
The session will crash if you minimize the browser window.

# TODO

Run Selenium in a docker container in the background.