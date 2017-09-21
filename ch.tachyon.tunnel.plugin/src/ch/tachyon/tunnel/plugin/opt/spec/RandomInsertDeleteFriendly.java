package ch.tachyon.tunnel.plugin.opt.spec;

import java.lang.annotation.*;

import ch.tachyon.tunnel.plugin.*;
import ch.tachyon.tunnel.plugin.host.*;

/**
 * Indicate that a plugin implementing the {@link IRandomAccessEffect} is "insert-delete-friendly", which means
 * that:
 * <ul>
 * <li>It does not perform writes (only insertions and deletions are used to modify the audio data)
 * <li>Insertions are only performed at the end of the audio region
 * <li>Reads and deletions are only performed at the beginning of the audio region
 * <li>No read is performed on a deleted or inserted part
 * </ul>
 * This annotation cannot be used together with {@link RandomReadWriteFriendly}. It can be used to simplify
 * the host's {@link IRandomAccessor} implementation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RandomInsertDeleteFriendly {

}
