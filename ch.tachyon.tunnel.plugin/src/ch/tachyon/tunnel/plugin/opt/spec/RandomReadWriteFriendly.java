package ch.tachyon.tunnel.plugin.opt.spec;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.host.*;

/**
 * Indicate that a plugin implementing the {@link IRandomAccessEffect} is "friendly", which means that:
 * <ul>
 * <li>It does not perform insertions or deletions
 * <li>It never reads samples that have already been written
 * </ul>
 * The presence of this annotation can simplify the host {@link IRandomAccessor} implementation, because
 * it ensures that reads will always read original data, even if reads and writes operate on the same
 * buffer.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RandomReadWriteFriendly {

}
