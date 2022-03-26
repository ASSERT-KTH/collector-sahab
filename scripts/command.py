import subprocess

class Command:

  cwd = None

  def __init__(self, cwd):
    self.cwd = cwd

  def git_checkout(self, commit):
    subprocess.run(["git", "checkout", commit], cwd=self.cwd)


  def mvn_test_compile(self):
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
