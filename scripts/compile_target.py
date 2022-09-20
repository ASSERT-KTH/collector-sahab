import argparse
import os

from command import Command
from revision import REVISION

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


def compile(project, commit, revision, dir_name):
  
  driver = Command(project)
  driver.clean(revision)

  driver.git_checkout(commit)

  driver.mvn_test_compile()
  driver.mvn_build_classpath()

  driver.rename(f"{revision.value.get_output_directory()}_{dir_name}")


def main():
  args = parser.parse_args()
  project_path = args.project
  commits = args.commits

  compile(project_path, commits[0], REVISION.LEFT, commits[1])
  compile(project_path, commits[1], REVISION.RIGHT, commits[1])

if __name__ == "__main__":
  main()
