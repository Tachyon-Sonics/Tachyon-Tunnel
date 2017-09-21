package ch.tachyon.tunnel.plugin.opt.doc;

import java.io.*;

/**
 * Interface for plugins that provide copyright and license information.
 * <p>
 * Hosts <b>must</b> honor this interface by providing a way for the user to read all the
 * information provided by this interface for any plugin that implements it.
 */
public interface ILicensed {

    /**
     * Author or company name.
     * Multiple authors can be separated by ", ".
     */
    public String getAuthor();

    /**
     * Copyright notice, usually <code>(C) <i>year</i> <i>author</i></code>.
     */
    public String getCopyright();

    /**
     * The name of the license. For an open source license this is the abbreviation and optional version,
     * such as <code>EPL</code>, <code>LGPL 2.0</code> or <code>LGPL 3.0 or greater</code>.
     * Can be <code>null</code> is the license terms are custom (such as a proprietary license or end-user
     * license agreement)
     */
    public String getLicenseName();

    /**
     * The license terms. If for example the License is the <code>EPL</code>, the returned reader should
     * allow the caller to fetch the entire license's content, in text or XHTML form, with UTF-8 encoding.
     * <p>
     * Licenses in XHTML form <b>must</b> starts with the XML header and be well-formed XHTML.
     * <p>
     * Can be <code>null</code>, in which case the plugin is "Copyright, all rights reserved" if a copyright
     * is given, public domain else. Should not be <code>null</code> if {@link #getLicenseName()} is given.
     */
    public Reader getLicenseTerms();

}
