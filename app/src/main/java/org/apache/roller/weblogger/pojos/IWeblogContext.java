package org.apache.roller.weblogger.pojos;

import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Interface representing the minimal weblog context needed by WeblogEntry.
 * This interface breaks the cyclic dependency between Weblog and WeblogEntry
 * by providing an abstraction layer.
 * 
 * @see Weblog
 * @see WeblogEntry
 */
public interface IWeblogContext {
    
    /**
     * Get the unique identifier for this weblog.
     * @return weblog ID
     */
    String getId();
    
    /**
     * Get the handle (short name/URL identifier) for this weblog.
     * @return weblog handle
     */
    String getHandle();
    
    /**
     * Get the locale instance for this weblog (for i18n).
     * @return locale
     */
    Locale getLocaleInstance();
    
    /**
     * Get initialized plugins for entry rendering.
     * @return map of entry plugins keyed by plugin name
     */
    Map<String, WeblogEntryPlugin> getInitializedPlugins();
    
    /**
     * Check if comments are allowed on this weblog.
     * @return true if comments allowed
     */
    Boolean getAllowComments();
    
    /**
     * Get the default/blogger category for this weblog.
     * @return the blogger category
     */
    WeblogCategory getBloggerCategory();
    
    /**
     * Get all categories for this weblog.
     * @return list of weblog categories
     */
    List<WeblogCategory> getWeblogCategories();
    
    /**
     * Get the locale string for this weblog.
     * @return locale string
     */
    String getLocale();
    
    /**
     * Get the timezone instance for this weblog.
     * @return timezone
     */
    TimeZone getTimeZoneInstance();
    
    /**
     * Check if a user has a specific permission on this weblog.
     * @param user the user to check
     * @param permission the permission to check
     * @return true if user has permission
     */
    boolean hasUserPermission(User user, String permission);
    
    /**
     * Get the name/title of this weblog.
     * @return weblog name
     */
    String getName();
}
