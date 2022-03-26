import argparse
from traceback import print_tb
from urllib import request

from unidiff import PatchSet

parser = argparse.ArgumentParser("Matched line locator")
parser.add_argument("-u", "--diff-url", required=True, help="Patch")


def run_unidiff(diff_url):
  diff = request.urlopen(diff_url)
  encoding = diff.headers.get_charsets()[0]
  patch = PatchSet(diff, encoding=encoding)
  result = []
  for file in patch:
    for hunk in file:
      required_lines_in_each_hunk = []
      for line in hunk:
        if not line.is_context and line.source_line_no is not None:
          required_lines_in_each_hunk.append(str(line.source_line_no))
      fully_qualified_name = get_fully_qualified_name(file.path)
      result.append((fully_qualified_name, required_lines_in_each_hunk))

  for output_for_java in result:
    print(f"{output_for_java[0]}={','.join(output_for_java[1])}")


def get_fully_qualified_name(path):
  required_string = path.split("java/")[1]
  required_string = required_string[:-5]
  return required_string.replace("/", ".")


def main():
  args = parser.parse_args()
  diff_url = args.diff_url
  run_unidiff(diff_url)


if __name__ == "__main__":
  main()