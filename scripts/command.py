import os
import subprocess
from glob import glob

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
    ], cwd=self.cwd)

  def mvn_build_classpath(self):
    subprocess.run([
      "mvn",
      "dependency:build-classpath",
      "-Dmdep.outputFile=target/cp.txt"
    ], cwd=self.cwd)

  def remove(self, dir):
    subprocess.run([
      "rm",
      "-rf",
      dir,
    ], cwd=self.cwd)

  def rename(self, renamed_directory):
    all_targets = glob(f"{self.cwd}/**/target/", recursive=True)
    for build_dir in all_targets:
      subprocess.run([
        "mv",
        build_dir,
        os.path.dirname(os.path.dirname(build_dir)) + f"/{renamed_directory}",
      ], cwd=self.cwd)

  def clean(self, revision):
    all_targets = glob(f"{self.cwd}/**/{revision.value.get_output_directory()}/", recursive=True)
    for build_dir in all_targets:
      self.remove(build_dir)

  def _replace_plugin_properties(self):
    # We replace them to empty strings because we are using user properties
    # above to set these properties.
    self._run_sed(r"<source>1\.5<\/source>", r"")
    self._run_sed(r"<target>1\.5<\/target>", r"")
    self._run_sed(r"<debug>false<\/debug>", r"")
    self._run_sed(r"<debuglevel>.*<\/debuglevel>", r"")
    self._run_sed(r"<maven\.compile\.source>1\.5<\/maven\.compile\.source>", r"<maven\.compile\.source>1\.6<\/maven\.compile\.source>")
    self._run_sed(r"<maven\.compile\.target>1\.5<\/maven\.compile\.target>", r"<maven\.compile\.target>1\.6<\/maven\.compile\.target>")


  def _run_sed(self, original, substitution):
    subprocess.run([
      "sed",
      "-i",
      f"s/{original}/{substitution}/",
      f"{os.path.join(self.cwd, 'pom.xml')}"
    ], cwd=self.cwd)
