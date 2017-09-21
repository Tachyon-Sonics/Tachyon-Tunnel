package ch.tachyon.tunnel.plugin.opt.doc;

import java.util.*;

/**
 * Allows a plugin to specify one or more tags to help searching functionnalities in hosts.
 * <p>
 * This class also defines constants for some standard tags and tag values.
 */
public interface ITagged {

    // todo: constants for standard tags

    /**
     * Get a list of tags for this plugins. Most relevant tags should appear first. A host is free
     * to only consider the <i>n</i> first tags.
     */
    public List<String> getTags();

}
