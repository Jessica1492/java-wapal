package wapal;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import java.time.Duration;

public class App {

    public static void main(String[] args) {

        final WebDriver driver = new FirefoxDriver();
        final WebDriverWait wait = new WebDriverWait(driver, 10L);
        try {
            // run on a local instance to avoid race conditions wrt image loading
            //driver.get("https://google.com/ncr");
            final String ic_path = System.getProperty("IC_PATH");
            System.out.println("Starting Incubus City from "+ic_path);
            driver.get(ic_path);
            // wait for SugarCube to finish loading
            final By by_button_begin = By.xpath("//button[text()='BEGIN']");
            final WebElement button_begin = wait.until(elementToBeClickable(by_button_begin));
            // wait until button is clickable
            // click "BEGIN"
            driver.findElement(by_button_begin).click();

            //
            final By by_button_begin2 = By.linkText("Tonight your epoch begins.");
            final WebElement firstResult = wait.until(elementToBeClickable(by_button_begin2));
            driver.findElement(by_button_begin2).click();

            final By by_n_impregnations = By.xpath("/html/body/div[4]/div[2]/div/div[2]/thick/strong");
            String nimpregs = driver.findElement(by_n_impregnations).getText();
            System.out.println("Impregnations: "+ nimpregs);
        } finally {
            driver.quit();
        }
    }
}
  
