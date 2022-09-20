from enum import Enum


class _Revision:
  def __init__(self, input_file, output_directory):
    self.input_file = input_file
    self.output_directory = output_directory
  
  def get_input_file(self):
    return self.input_file

  def get_output_directory(self):
    return self.output_directory


class REVISION(Enum):
  LEFT = _Revision("input-left", "sahab-left")
  RIGHT = _Revision("input-right", "sahab-right")
