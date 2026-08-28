def approved_string($limit):
  if type == "string" then .[0:$limit] else null end;

if type == "object" then
  {
    error: (.error | approved_string(256)),
    description: (.description | approved_string(2048))
  }
else
  {error: null, description: null}
end
