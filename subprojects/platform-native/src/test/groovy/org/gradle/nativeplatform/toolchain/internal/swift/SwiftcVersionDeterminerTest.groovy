/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativeplatform.toolchain.internal.swift

import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecAction
import org.gradle.process.internal.ExecActionFactory
import spock.lang.Specification

class SwiftcVersionDeterminerTest extends Specification {

    def execActionFactory = Mock(ExecActionFactory)

    private static final String SWIFTC_OUTPUT_MAC_OS = """Apple Swift version 4.0.2 (swiftlang-900.0.69.2 clang-900.0.38)
Target: x86_64-apple-macosx10.9        
    """

    private static final String SWIFTC_OUTPUT_LINUX = """Swift version 3.1.1 (swift-3.1.1-RELEASE)
Target: x86_64-unknown-linux-gnu       
    """

    def "returns correct version"() {
        expect:
        output(SWIFTC_OUTPUT_MAC_OS).versionString == 'Apple Swift version 4.0.2 (swiftlang-900.0.69.2 clang-900.0.38)'
        output(SWIFTC_OUTPUT_LINUX).versionString == 'Swift version 3.1.1 (swift-3.1.1-RELEASE)'
    }

    SwiftcVersionResult output(String output) {
        def action = Mock(ExecAction)
        def result = Mock(ExecResult)
        1 * execActionFactory.newExecAction() >> action
        1 * action.setStandardOutput(_) >> { OutputStream outstr -> outstr << output; action }
        1 * action.execute() >> result
        new SwiftcVersionDeterminer(execActionFactory).getSwiftcMetaData(new File("swiftc"))
    }

}
