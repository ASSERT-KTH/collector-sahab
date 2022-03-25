import argparse
import os
import subprocess
from enum import Enum

class VerifyDirectory(argparse.Action):
  def __call__(self, parser, namespace, directory, option_string=None):
    if not os.path.isdir(directory):
        raise argparse.ArgumentTypeError(f"{directory} is not a valid directory")
    if not "pom.xml" in os.listdir(directory):
      raise argparse.ArgumentTypeError(f"{directory} is not a maven project")
    setattr(namespace, self.dest, directory)

parser = argparse.ArgumentParser("Prepare project")
parser.add_argument("-p", "--project", action=VerifyDirectory, required=True, help="Path to project")
parser.add_argument("-c", "--commits", nargs=2, required=True, help="Revisions of the project")


class REVISION(Enum):
  LEFT = "sahab-left"
  RIGHT = "sahab-right"

class Commands:

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


def compile(project, commit, revision):
  
  driver = Commands(project)
  driver.git_checkout(commit)

  driver.mvn_test_compile()
  driver.mvn_copy_dependencies()

  driver.rename(revision)


def main():
  args = parser.parse_args()
  project_path = args.project
  commits = args.commits
  compile(project_path, commits[0], REVISION.LEFT)
  compile(project_path, commits[1], REVISION.RIGHT)

if __name__ == "__main__":
  main()
