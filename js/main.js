let query = ''

async function main() {
  const response = await fetch('data.json')
  const json     = await response.json()
  const data     = formatData(json)

  const clusterize = new Clusterize({
    rows: filterData(data, query),
    scrollId: 'scroll-area',
    contentId: 'content-area',
    rows_in_block: 100
  })

  const search = document.getElementById('search')
  search.oninput = () => {
    const query = search.value.toLowerCase();

    data.forEach(item => {
      item.active = query.length == 0                                 || 
                    item.pack.title.toLowerCase().indexOf(query) >= 0 ||
                    substringOfArray(query, item.pack.tags) 
    })

    clusterize.update(filterData(data))
  }
}

function formatData(json) {
  const source   = document.getElementById('pack-template').innerHTML;
  const template = Handlebars.compile(source);
  const data     = []

  json.packs.forEach(pack => {
    const html = template(pack)
    data.push({
      pack: pack,
      html: html,
      active: true
    })
  }) 

  return data
}

function filterData(data) {
  return data.filter(item => item.active)
             .map(item => item.html)
}

function substringOfArray(query, array) {
  for (let i = 0; i < array.length; i++) {
    if (array[i].indexOf(array) >= 0) {
      return true;
    }
  }
  
  return false
}

function onPackClicked(url) {
  window.location.href = url 
}

main()
