import os
import subprocess

class Command:

  cwd = None

  def __init__(self, cwd):
    self.cwd = cwd

  def git_checkout(self, commit):
    subprocess.run(["git", "checkout", commit], cwd=self.cwd)


  def mvn_test_compile(self):
    self._replace_plugin_properties()
    subprocess.run([
      "mvn",
      "test-compile",
      "-Dmaven.compiler.debug=true",
      "-Dmaven.compiler.debuglevel=lines,vars,source",
      "-Dmaven.compiler.source=1.6",
      "-Dmaven.compiler.target=1.6",
    ], cwd=self.cwd)

  def mvn_copy_dependencies(self):
    subprocess.run([
      "mvn",
      "dependency:copy-dependencies"
    ], cwd=self.cwd)

  def rename(self, renamed_directory):
    subprocess.run([
      "mv",
      "target",
      renamed_directory.value,
    ], cwd=self.cwd)


  def _replace_plugin_properties(self):
    # We replace them to empty strings because we are using user properties
    # above to set these properties.
    self._run_sed(r"<source>1\.5<\/source>", r"")
    self._run_sed(r"<target>1\.5<\/target>", r"")
    self._run_sed(r"<debug>false<\/debug>", r"")
    self._run_sed(r"<debuglevel>.*<\/debuglevel>", r"")


  def _run_sed(self, original, substitution):
    subprocess.run([
      "sed",
      "-i",
      f"s/{original}/{substitution}/",
      f"{os.path.join(self.cwd, 'pom.xml')}"
    ], cwd=self.cwd)
