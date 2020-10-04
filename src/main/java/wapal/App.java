package wapal;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

import java.util.List;

public class App {

    static final By BY_BUTTON_BEGIN = By.xpath("//button[text()='BEGIN']");
    static final By BY_LINK_EPOCH_BEGINS = By.linkText("Tonight your epoch begins.");

    public static void main(String[] args) {

        final WebDriver driver = new FirefoxDriver();
        final WebDriverWait wait = new WebDriverWait(driver, 10L);
        try {
            // run on a local instance to avoid race conditions wrt image loading
            //driver.get("https://google.com/ncr");
            final String ic_path = System.getProperty("IC_PATH");
            if ( ic_path == null || ic_path.isEmpty() ) {
                System.err.println("You MUST point environment variable IC_PATH to the location of your Incubus City installation.");
                System.exit(1);
            }
            System.out.println("Starting Incubus City from "+ic_path);
            driver.get(ic_path);
            
            System.out.println("Waiting for SugarCube to finish loading...");
            // wait until button is clickable
            // click "BEGIN"
            wait.until(elementToBeClickable(BY_BUTTON_BEGIN)).click();
            
            //
            wait.until(elementToBeClickable(BY_LINK_EPOCH_BEGINS)).click();

            check_for_main_menu( driver, wait );
        } finally {
            driver.quit();
        }
    }

    static final By BY_MAIN_TITLE = By.xpath("/html/body/div[5]/div/div/p");
    static final By BY_N_IMPREGNATIONS = By.xpath("/html/body/div[4]/div[2]/div/div[2]/thick/strong");

    static void check_for_main_menu( WebDriver driver, WebDriverWait wait ) {

            final String nimpregs = wait.until(presenceOfElementLocated(BY_N_IMPREGNATIONS)).getText();
            System.out.println("Impregnations: "+ nimpregs);

            driver.findElement(BY_MAIN_TITLE).getText().equals("In your mind's eye, fertile wombs glitter like gems all around you. You sense...");
            // TODO: handle element not found
        
            final List<WebElement> passages = driver.findElement(By.id("passages")).findElements(By.className("macro-link"));
            System.out.println("Passages available:");
            passages.stream().map( WebElement::getText ).forEach( System.out::println );
    }
}
  
