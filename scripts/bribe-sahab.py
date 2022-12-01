import argparse
import json
import os
import shlex
import subprocess
from glob import glob

from revision import REVISION
from compile_target import compile

CONFIG = json.load(open(os.path.join(os.path.dirname(__file__), 'config.json')))

COLLECTOR_JAR = CONFIG.get('collectorJar', None)
OUTPUT_DIRECTORY = CONFIG.get('outputDirectory', '')

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
parser.add_argument("--execution-depth", type=int, default=0, help="Execution depth")

# Specifically for finding matched lines
parser.add_argument("-c", "--class-filename", required=True, help="Name of the file which contains the patch")

# For collector sahab
parser.add_argument("-t", "--tests", required=True, nargs="+", help="Tests executed by collector sahab")


def _compile_target(project, left, right):
  compile(project, left, REVISION.LEFT, right)
  compile(project, right, REVISION.RIGHT, right)


def _find_matched_lines(project, filename, left, right):
  cmd = (
    "java "
    f"-classpath {COLLECTOR_JAR} "
    "se.kth.debug.MatchedLineFinder "
    f"{project} {shlex.quote(filename)} {left} {right}"
  )
  subprocess.run(cmd, shell=True)


def _get_or_create_directory_for_creating_output_files(ref):
  output_directory = os.path.join(OUTPUT_DIRECTORY, "sahab-reports", ref)
  if not os.path.exists(output_directory):
    os.makedirs(os.path.join(OUTPUT_DIRECTORY, "sahab-reports", ref))
  return output_directory

def _run_collector_sahab(project, tests, revision, ref, execution_depth):
  all_targets = glob(f"{project}/**/{revision.value.get_output_directory()}_{ref}/", recursive=True)
  all_dependencies = []
  project_maven_dependencies = []
  for build_dir in all_targets:
    # We put our dependencies first in the list so that our built classes get
    # precedence.
    all_dependencies.append(os.path.join(build_dir, 'classes'))
    all_dependencies.append(os.path.join(build_dir, 'test-classes'))

    # google-java-format has a module called 'eclipse_plugin' which has dependencies that
    # override classes in the module core. This prevents out JUnitTestRunner from running tests.
    if 'google-java-format/eclipse_plugin' in build_dir:
        continue
    try:
        with open(os.path.join(build_dir, 'cp.txt')) as cp:
          classpath = cp.read().strip()
          project_maven_dependencies.extend(classpath.split(':'))
    except FileNotFoundError:
        print(f'No cp.txt found in {build_dir}')

  all_dependencies.extend(project_maven_dependencies)

  test_methods = " ".join(tests)
  output_directory = _get_or_create_directory_for_creating_output_files(ref)
  collector_sahab_output = os.path.join(output_directory, f"{revision.name.lower()}.json")

  body_of_argument_file = (
    f"-i {revision.value.get_input_file()}_{ref}.txt "
    f"-p {' '.join(all_dependencies)} "
    f"-t {test_methods} "
    f"-o {collector_sahab_output} "
    f"-m methods_{ref}.json "
    f"--execution-depth={execution_depth}"
  )

  path_to_argument_file = os.path.join(project, f'{revision.value.get_output_directory()}_af.txt')

  with open(path_to_argument_file, "w+") as f:
    f.write(body_of_argument_file)

  cmd = (
    "java "
    f"-jar {COLLECTOR_JAR} "
    f"@{path_to_argument_file}"
  )
  print(cmd)

  subprocess.run(cmd, shell=True)


def main():
  args = parser.parse_args()
  project = args.project
  left_revision_ref = args.left
  right_revision_ref = args.right

  execution_depth = args.execution_depth

  classname = args.class_filename

  tests = args.tests

  _compile_target(project, left_revision_ref, right_revision_ref)
  _find_matched_lines(project, classname, left_revision_ref, right_revision_ref)
  _run_collector_sahab(project, tests, REVISION.LEFT, right_revision_ref, execution_depth)
  _run_collector_sahab(project, tests, REVISION.RIGHT, right_revision_ref, execution_depth)

if __name__ == "__main__":
  main()
