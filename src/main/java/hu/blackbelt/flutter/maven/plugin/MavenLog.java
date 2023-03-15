package hu.blackbelt.flutter.maven.plugin;

/*-
 * #%L
 * flutter-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
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
 * #L%
 */

public class MavenLog implements Log {

	final org.apache.maven.plugin.logging.Log log;
	
	public MavenLog(org.apache.maven.plugin.logging.Log log) {
		this.log = log;
	}

	public void debug(CharSequence content) {
		log.debug(content);
	}

	public void debug(CharSequence content, Throwable error) {
		log.debug(content, error);
	}

	public void debug(Throwable error) {
		log.debug(error);
	}

	public void info(CharSequence content) {
		log.info(content);
	}

	public void info(CharSequence content, Throwable error) {
		log.info(content, error);
	}

	public void info(Throwable error) {
		log.info(error);
	}

	public void warn(CharSequence content) {
		log.warn(content);
	}

	public void warn(CharSequence content, Throwable error) {
		log.warn(content, error);
	}

	public void warn(Throwable error) {
		log.warn(error);
	}

	public void error(CharSequence content) {
		log.error(content);
	}

	public void error(CharSequence content, Throwable error) {
		log.error(content, error);
	}

	public void error(Throwable error) {
		log.error(error);
	}

}
