/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.provider.MemoryManager;
import org.jcodings.Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.exception.CoreExceptions;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.CoreStrings;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.platform.posix.Sockets;
import org.jruby.truffle.platform.posix.TrufflePosix;
import org.jruby.util.ByteList;

@TypeSystemReference(RubyTypes.class)
@ImportStatic(RubyGuards.class)
public abstract class RubyBaseNode extends Node {

    @CompilationFinal private RubyContext context;

    private boolean atNewline = false;

    public RubyBaseNode() {
    }

    public RubyBaseNode(RubyContext context, SourceSection sourceSection) {
        super(sourceSection);
        assert context != null;
        this.context = context;
    }

    // Guards which use the context and so can't be static

    protected boolean isNil(Object value) {
        return value == nil();
    }

    protected boolean isRubiniusUndefined(Object value) {
        return value == coreLibrary().getRubiniusUndefined();
    }

    protected DynamicObjectFactory getInstanceFactory(DynamicObject rubyClass) {
        return Layouts.CLASS.getInstanceFactory(rubyClass);
    }

    // Helpers methods for terseness

    protected DynamicObject nil() {
        return coreLibrary().getNilObject();
    }

    protected DynamicObject getSymbol(String name) {
        return getContext().getSymbolTable().getSymbol(name);
    }

    protected DynamicObject getSymbol(Rope name) {
        return getContext().getSymbolTable().getSymbol(name);
    }

    protected DynamicObject createString(ByteList bytes) {
        return StringOperations.createString(getContext(), bytes);
    }

    protected DynamicObject create7BitString(CharSequence value, Encoding encoding) {
        return StringOperations.createString(getContext(), StringOperations.encodeRope(value, encoding, CodeRange.CR_7BIT));
    }

    protected DynamicObject createString(Rope rope) {
        return StringOperations.createString(getContext(), rope);
    }

    protected CoreStrings coreStrings() {
        return getContext().getCoreStrings();
    }

    protected CoreLibrary coreLibrary() {
        return getContext().getCoreLibrary();
    }

    protected CoreExceptions coreExceptions() {
        return getContext().getCoreExceptions();
    }

    protected TrufflePosix posix() {
        return getContext().getNativePlatform().getPosix();
    }

    protected Sockets nativeSockets() {
        return getContext().getNativePlatform().getSockets();
    }

    protected MemoryManager memoryManager() {
        return getContext().getNativePlatform().getMemoryManager();
    }

    protected Object ruby(String expression, Object... arguments) {
        return getContext().getCodeLoader().inline(this, expression, arguments);
    }

    // Accessors

    public RubyContext getContext() {
        if (context == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            Node parent = getParent();

            while (true) {
                if (parent == null) {
                    throw new UnsupportedOperationException("can't get the RubyContext because the parent is null");
                }

                if (parent instanceof RubyBaseNode) {
                    context = ((RubyBaseNode) parent).getContext();
                    break;
                }

                if (parent instanceof RubyRootNode) {
                    context = ((RubyRootNode) parent).getContext();
                    break;
                }

                parent = parent.getParent();
            }

        }

        return context;
    }

    public void setAtNewline() {
        atNewline = true;
    }

    public boolean isAtNewline() {
        return atNewline;
    }

}