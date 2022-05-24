import argparse
import os
import subprocess

from revision import REVISION
from compile_target import compile

COLLECTOR_JAR = "/home/assert/Desktop/assert-achievements/collector-sahab/target/collector-sahab-0.0.1-jar-with-dependencies.jar"
OUTPUT_DIRECTORY = "/home/assert/Desktop/experiments/drr-as-pr/"

class VerifyDirectory(argparse.Action):
  def __call__(self, parser, namespace, directory, option_string=None):
    if not os.path.isdir(directory):
        raise argparse.ArgumentTypeError(f"{directory} is not a valid directory")
    if not "pom.xml" in os.listdir(directory):
      raise argparse.ArgumentTypeError(f"{directory} is not a maven project")
    setattr(namespace, self.dest, directory)

parser = argparse.ArgumentParser("Bribe sahab")
parser.add_argument("-p", "--project", action=VerifyDirectory, required=True, help="Path to project")
parser.add_argument("-l", "--left", required=True, help="Left revision")
parser.add_argument("-r", "--right", required=True, help="Right revision")

# Specifically for finding matched lines
parser.add_argument("-c", "--class-filename", required=True, help="Name of the file which contains the patch")

# For collector sahab
parser.add_argument("-t", "--tests", required=True, nargs="+", help="Tests executed by collector sahab")


def _compile_target(project, left, right):
  compile(project, left, REVISION.LEFT)
  compile(project, right, REVISION.RIGHT)


def _find_matched_lines(project, filename, left, right):
  cmd = (
    "java "
    f"-classpath {COLLECTOR_JAR} "
    "se.kth.debug.MatchedLineFinder "
    f"{project} {filename} {left} {right}"
  )
  subprocess.run(cmd, shell=True)


def _get_or_create_directory_for_creating_output_files(ref):
  output_directory = os.path.join(OUTPUT_DIRECTORY, "sahab-reports", ref)
  if not os.path.exists(output_directory):
    os.makedirs(os.path.join(OUTPUT_DIRECTORY, "sahab-reports", ref))
  return output_directory

def _run_collector_sahab(project, tests, revision, ref):
  project_dependencies = [
    os.path.join(project, revision.value.get_output_directory(), 'classes'),
    os.path.join(project, revision.value.get_output_directory(), 'test-classes'),
    os.path.join(project, revision.value.get_output_directory(), 'dependency'),
  ]
  test_methods = " ".join(tests)
  output_directory = _get_or_create_directory_for_creating_output_files(ref)
  collector_sahab_output = os.path.join(output_directory, f"{revision.name.lower()}.json")

  cmd = (
    "java "
    f"-jar {COLLECTOR_JAR} "
    f"-i {revision.value.get_input_file()} "
    f"-p {' '.join(project_dependencies)} "
    f"-t {test_methods} "
    f"-o {collector_sahab_output} "
    "--execution-depth 1"
  )

  subprocess.run(cmd, shell=True)


def main():
  args = parser.parse_args()
  project = args.project
  left_revision_ref = args.left
  right_revision_ref = args.right

  classname = args.class_filename

  tests = args.tests

  _compile_target(project, left_revision_ref, right_revision_ref)
  _find_matched_lines(project, classname, left_revision_ref, right_revision_ref)
  _run_collector_sahab(project, tests, REVISION.LEFT, right_revision_ref)
  _run_collector_sahab(project, tests, REVISION.RIGHT, right_revision_ref)

if __name__ == "__main__":
  main()
