export class Network{
	private _title: String;
	private _nodes: any[];
	private _interactions: any[];
	
	constructor(){
		this._title = 'Network';
		this._nodes = [
			{	group: 'nodes',	classes: 'mRNA'    , data: { id: "NM_000956"      ,label: "PTGER2"         ,weight:  -8.262, color: '#007f7f', borderColor: '#007f7f' },	style: { shape: 'ellipse' }	},
			{	group: 'nodes',	classes: 'mRNA'    , data: { id: "NM_022051"      ,label: "EGLN1"          ,weight:  6.7884, color: '#007f7f', borderColor: '#007f7f' },	style: { shape: 'ellipse' }	},
			{	group: 'nodes', classes: 'mRNA'    , data: { id: "NM_000594"      ,label: "TNF"            ,weight:  7.5229, color: '#007f7f', borderColor: '#007f7f' },	style: { shape: 'ellipse'	}	},
			{ group: 'nodes', classes: 'mRNA'    , data: { id: "NM_014009"      ,label: "FOXP3"          ,weight:  7.3486, color: '#007f7f', borderColor: '#007f7f' },	style: { shape: 'ellipse' }	},	
			{	group: 'nodes',	classes: 'mRNA'    , data: { id: 'NM_002314'      ,label: 'LIMK1'          ,weight:  8.1765, color: '#007f7f', borderColor: '#007f7f' },	style: { shape: 'ellipse' }	},
			{ group: 'nodes', classes: 'mRNA'    , data: { id: "NM_001042425"   ,label: 'TFAP2A'         ,weight:  7.3468, color: '#007f7f', borderColor: '#007f7f' },	style: { shape: 'ellipse '} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-149-5p" ,label: 'hsa-miR-149-5p' ,weight:  4.2110, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-187-3p" ,label: 'hsa-miR-187-3p' ,weight:  3.3347, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-31-5p"  ,label: 'hsa-miR-31-5p'  ,weight:  3.2417, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-19a-3p" ,label: 'hsa-miR-19a-3p' ,weight:  1.0244, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-210-3p" ,label: 'hsa-miR-210-3p' ,weight:  0.8138, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-193a-5p",label: 'hsa-miR-193a-5p',weight:  0.5424, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-17-5p"  ,label: 'hsa-miR-17-5p'  ,weight:  0.3686, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-21-5p"  ,label: 'hsa-miR-21-5p'  ,weight:  0.2753, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-20a-5p" ,label: 'hsa-miR-20a-5p' ,weight:  0.2267, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-130a-3p",label: 'hsa-miR-130a-3p',weight:  0.1480, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-125b-5p",label: 'hsa-miR-125b-5p',weight:  -0.115, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			{ group: 'nodes', classes: 'microRNA', data: { id: "hsa-miR-143-3p" ,label: 'hsa-miR-143-3p' ,weight:  -0.137, color: '#827f00', borderColor: '#827f00' },	style: { shape: 'pentagon'} },
			
		];
		this._interactions = [
			{ group: 'edges', data: { id: 'edg0' , source: "hsa-miR-143-3p" , target: "NM_002314"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg1' , source: "hsa-miR-20a-5p" , target: "NM_002314"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg5' , source: "hsa-miR-210-3p" , target: "NM_014009"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg6' , source: "hsa-miR-31-5p"  , target: "NM_014009"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg7' , source: "hsa-miR-17-5p"  , target: "NM_000594"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg8' , source: "hsa-miR-125b-5p", target: "NM_000594"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg9' , source: "hsa-miR-143-3p" , target: "NM_000594"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg10', source: "hsa-miR-130a-3p", target: "NM_000594"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg11', source: "hsa-miR-187-3p" , target: "NM_000594"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg12', source: "hsa-miR-19a-3p" , target: "NM_000594"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg13', source: "hsa-miR-21-5p"  , target: "NM_022051"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg14', source: "hsa-miR-149-5p" , target: "NM_000956"   , color: '#989898' } },
			{ group: 'edges', data: { id: 'edg15', source: "hsa-miR-193a-5p", target: "NM_001042425", color: '#989898' } },
		];
	}

	public getInteractions(): any[]{
		return this._interactions;
	}
	
	public getNodes(): any[]{
		return this._nodes;
	}
}